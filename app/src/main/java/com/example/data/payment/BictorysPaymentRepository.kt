package com.example.data.payment

import android.util.Log
import com.example.data.local.PreferencesRepository
import com.example.data.repository.ConcoursRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed interface PaymentInitiationResult {
    data class CheckoutReady(
        val record: PendingPaymentRecord,
        val checkoutUrl: String
    ) : PaymentInitiationResult

    data class DirectPushInitiated(
        val record: PendingPaymentRecord
    ) : PaymentInitiationResult

    data class Error(val message: String, val missingApiKey: Boolean = false) : PaymentInitiationResult
}

sealed interface PaymentVerificationResult {
    data object NoPendingPayment : PaymentVerificationResult
    data class Succeeded(val record: PendingPaymentRecord) : PaymentVerificationResult
    data class StillPending(val record: PendingPaymentRecord) : PaymentVerificationResult
    data class Failed(val message: String) : PaymentVerificationResult
    data class Error(val message: String) : PaymentVerificationResult
}

class BictorysPaymentRepository(
    private val apiService: BictorysApiService,
    private val preferencesRepository: PreferencesRepository,
    private val concoursRepository: ConcoursRepository,
    private val apiKeyProvider: () -> String = { BictorysConfig.apiKey() }
) {
    private val moshi = BictorysApiService.createMoshi()
    private val pendingAdapter = moshi.adapter(PendingPaymentRecord::class.java)

    val catalog: List<StorePack> = listOf(
        StorePack(
            id = "pack_soutien",
            title = "Pack Soutien",
            subtitle = "+120 Gemmes pour recharger tes cœurs",
            badge = null,
            amountXof = 500,
            gemsReward = 120,
            refillsHearts = false,
            grantsPass = false
        ),
        StorePack(
            id = "pack_major",
            title = "Pack Major",
            subtitle = "+400 Gemmes et 5 cœurs restaurés",
            badge = "Populaire",
            amountXof = 1500,
            gemsReward = 400,
            refillsHearts = true,
            grantsPass = false
        ),
        StorePack(
            id = "pass_concours",
            title = "Pass ConcoursGo",
            subtitle = "+650 Gemmes, 5 cœurs & badge Pass actif",
            badge = "Meilleure offre",
            amountXof = 2500,
            gemsReward = 650,
            refillsHearts = true,
            grantsPass = true
        )
    )

    val pendingPaymentFlow: Flow<PendingPaymentRecord?> = preferencesRepository.preferencesFlow.map { prefs ->
        decodePending(prefs.pendingPaymentJson)
    }

    private fun decodePending(json: String): PendingPaymentRecord? {
        if (json.isBlank()) return null
        return try {
            pendingAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun savePending(record: PendingPaymentRecord?) {
        val encoded = if (record == null) "" else pendingAdapter.toJson(record)
        preferencesRepository.setPendingPaymentJson(encoded)
    }

    suspend fun clearPendingPayment() {
        savePending(null)
    }

    suspend fun initiatePayment(
        pack: StorePack,
        customerName: String,
        customerEmail: String?,
        customerPhone: String,
        countryCode: String,
        paymentMethod: BictorysPaymentMethod
    ): PaymentInitiationResult {
        val apiKey = apiKeyProvider().trim()
        if (!BictorysConfig.isConfigured(apiKey)) {
            return PaymentInitiationResult.Error(
                message = "Clé API Bictorys manquante. Configure BICTORYS_PUBLIC_API_KEY dans les Secrets AI Studio.",
                missingApiKey = true
            )
        }

        BictorysValidator.validateCustomerName(customerName)?.let {
            return PaymentInitiationResult.Error(it)
        }
        BictorysValidator.validatePhone(customerPhone)?.let {
            return PaymentInitiationResult.Error(it)
        }
        val cleanEmail = customerEmail?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanEmail != null) {
            BictorysValidator.validateEmail(cleanEmail)?.let {
                return PaymentInitiationResult.Error(it)
            }
        }
        val safeCountry = countryCode.trim().uppercase()
        if (!BictorysValidator.isAllowedCountry(safeCountry)) {
            return PaymentInitiationResult.Error("Code pays non supporté.")
        }

        val prefs = preferencesRepository.getCurrentPreferences()
        if (prefs.offlineModeEnabled) {
            return PaymentInitiationResult.Error("Désactive le mode hors-ligne pour effectuer un paiement Bictorys.")
        }
        if (!concoursRepository.isNetworkConnected()) {
            return PaymentInitiationResult.Error("Aucune connexion internet détectée.")
        }

        val reference = "CG-${UUID.randomUUID().toString().replace("-", "").take(10).uppercase()}"
        val request = BictorysCreateChargeRequest(
            amount = pack.amountXof,
            currency = "XOF",
            country = safeCountry,
            paymentReference = reference,
            customerObject = BictorysCustomerObject(
                name = customerName.trim(),
                email = cleanEmail,
                phone = BictorysValidator.normalizePhone(customerPhone),
                country = safeCountry
            ),
            orderDetails = listOf(
                BictorysOrderDetail(
                    name = pack.title,
                    price = pack.amountXof,
                    quantity = 1
                )
            )
        )

        return try {
            val response = apiService.createCharge(
                apiKey = apiKey,
                paymentType = paymentMethod.queryParam,
                request = request
            )
            if (!response.isSuccessful) {
                val code = response.code()
                val errBody = response.errorBody()?.string()?.take(180).orEmpty()
                Log.w(TAG, "Bictorys createCharge HTTP $code")
                val msg = when (code) {
                    401, 403 -> "Clé API Bictorys refusée ($code). Vérifie BICTORYS_PUBLIC_API_KEY."
                    400, 422 -> "Paramètres de paiement invalides ($code). ${errBody.ifBlank { "" }}".trim()
                    else -> "Erreur serveur Bictorys ($code). Réessaie dans un instant."
                }
                return PaymentInitiationResult.Error(msg)
            }

            val body = response.body()
                ?: return PaymentInitiationResult.Error("Réponse vide du serveur Bictorys.")

            val txId = body.resolvedTransactionId() ?: reference
            val rawUrl = body.resolvedCheckoutUrl()

            if (rawUrl != null && !BictorysValidator.isTrustedCheckoutUrl(rawUrl)) {
                Log.w(TAG, "Untrusted checkout URL rejected")
                return PaymentInitiationResult.Error("URL de paiement Bictorys non reconnue.")
            }

            val record = PendingPaymentRecord(
                chargeId = body.chargeId ?: txId,
                transactionId = txId,
                paymentReference = reference,
                packId = pack.id,
                packTitle = pack.title,
                amountXof = pack.amountXof,
                gemsReward = pack.gemsReward,
                refillsHearts = pack.refillsHearts,
                grantsPass = pack.grantsPass,
                checkoutUrl = rawUrl.orEmpty(),
                status = body.status?.takeIf { it.isNotBlank() } ?: "initiated",
                createdAtMillis = System.currentTimeMillis(),
                credited = false
            )
            savePending(record)

            if (rawUrl != null) {
                PaymentInitiationResult.CheckoutReady(record = record, checkoutUrl = rawUrl)
            } else {
                PaymentInitiationResult.DirectPushInitiated(record = record)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Bictorys createCharge failed", e)
            PaymentInitiationResult.Error(
                e.localizedMessage ?: "Impossible de contacter la passerelle Bictorys."
            )
        }
    }

    suspend fun verifyPendingPayment(currentUserId: String?): PaymentVerificationResult {
        val prefs = preferencesRepository.getCurrentPreferences()
        val pending = decodePending(prefs.pendingPaymentJson)
            ?: return PaymentVerificationResult.NoPendingPayment

        if (pending.credited) {
            savePending(null)
            return PaymentVerificationResult.Succeeded(pending)
        }

        val apiKey = apiKeyProvider().trim()
        if (!BictorysConfig.isConfigured(apiKey)) {
            return PaymentVerificationResult.Error("Clé API Bictorys manquante.")
        }
        if (prefs.offlineModeEnabled || !concoursRepository.isNetworkConnected()) {
            return PaymentVerificationResult.Error("Connexion internet requise pour vérifier le paiement.")
        }

        return try {
            val statusResp = apiService.getTransactionStatus(
                apiKey = apiKey,
                transactionId = pending.transactionId
            )
            val resolvedBody = if (statusResp.isSuccessful && statusResp.body() != null) {
                statusResp.body()
            } else {
                val searchResp = apiService.searchTransactionByReference(
                    apiKey = apiKey,
                    paymentReference = pending.paymentReference
                )
                if (searchResp.isSuccessful) searchResp.body() else null
            }

            if (resolvedBody == null) {
                return PaymentVerificationResult.StillPending(pending)
            }

            val rawStatus = resolvedBody.status ?: pending.status
            when (BictorysValidator.normalizeStatus(rawStatus)) {
                NormalizedPaymentStatus.SUCCEEDED -> {
                    concoursRepository.applyPurchasedReward(
                        gemsToAdd = pending.gemsReward,
                        refillHearts = pending.refillsHearts,
                        grantsPass = pending.grantsPass,
                        currentUserId = currentUserId
                    )
                    val completedRecord = pending.copy(status = rawStatus, credited = true)
                    savePending(null)
                    preferencesRepository.setLastPaymentReceipt(
                        "${pending.packTitle} (${pending.amountXof} XOF) • Réf. ${pending.paymentReference}"
                    )
                    PaymentVerificationResult.Succeeded(completedRecord)
                }

                NormalizedPaymentStatus.FAILED -> {
                    savePending(null)
                    PaymentVerificationResult.Failed(
                        "Le paiement (${pending.paymentReference}) a échoué ou expiré ($rawStatus)."
                    )
                }

                NormalizedPaymentStatus.PENDING -> {
                    val updated = pending.copy(status = rawStatus)
                    savePending(updated)
                    PaymentVerificationResult.StillPending(updated)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Bictorys verify failed", e)
            PaymentVerificationResult.Error(
                e.localizedMessage ?: "Erreur lors de la vérification du paiement."
            )
        }
    }

    private companion object {
        const val TAG = "BictorysPaymentRepo"
    }
}
