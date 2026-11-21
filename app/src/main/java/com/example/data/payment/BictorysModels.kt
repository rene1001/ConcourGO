package com.example.data.payment

import com.example.BuildConfig
import com.squareup.moshi.Json
import java.net.URI
import java.util.Locale

object BictorysConfig {
    const val PLACEHOLDER_KEY = "MY_BICTORYS_PUBLIC_API_KEY"
    const val SANDBOX_BASE_URL = "https://api.test.bictorys.com/"
    const val LIVE_BASE_URL = "https://api.bictorys.com/"
    const val SUCCESS_REDIRECT_URL = "https://concoursgo.app/payment/success"
    const val ERROR_REDIRECT_URL = "https://concoursgo.app/payment/error"

    fun apiKey(): String = BuildConfig.BICTORYS_PUBLIC_API_KEY.trim()

    fun isConfigured(key: String = apiKey()): Boolean =
        key.isNotBlank() && key != PLACEHOLDER_KEY && !key.startsWith("MY_")

    fun isSandboxKey(key: String = apiKey()): Boolean =
        key.startsWith("test_", ignoreCase = true)

    fun resolveBaseUrl(key: String = apiKey()): String =
        if (isSandboxKey(key) || !isConfigured(key)) SANDBOX_BASE_URL else LIVE_BASE_URL
}

data class BictorysCustomerObject(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val country: String = "BF"
)

data class BictorysOrderDetail(
    val name: String,
    val price: Int,
    val quantity: Int = 1
)

data class BictorysCreateChargeRequest(
    val amount: Int,
    val currency: String = "XOF",
    val country: String = "BF",
    val paymentReference: String,
    val successRedirectUrl: String = BictorysConfig.SUCCESS_REDIRECT_URL,
    val errorRedirectUrl: String = BictorysConfig.ERROR_REDIRECT_URL,
    val customerObject: BictorysCustomerObject,
    val orderDetails: List<BictorysOrderDetail>
)

data class BictorysCreateChargeResponse(
    val chargeId: String? = null,
    val id: String? = null,
    val transactionId: String? = null,
    val link: String? = null,
    val redirectUrl: String? = null,
    val status: String? = null,
    val message: String? = null
) {
    fun resolvedTransactionId(): String? =
        transactionId?.takeIf { it.isNotBlank() }
            ?: chargeId?.takeIf { it.isNotBlank() }
            ?: id?.takeIf { it.isNotBlank() }

    fun resolvedCheckoutUrl(): String? =
        link?.takeIf { it.isNotBlank() }
            ?: redirectUrl?.takeIf { it.isNotBlank() }
}

data class BictorysTransactionStatusResponse(
    val id: String? = null,
    val chargeId: String? = null,
    val transactionId: String? = null,
    val status: String? = null,
    val amount: Int? = null,
    val currency: String? = null,
    @Json(name = "payment_reference") val paymentReferenceSnake: String? = null,
    val paymentReference: String? = null,
    val message: String? = null
)

data class StorePack(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val amountXof: Int,
    val gemsReward: Int,
    val refillsHearts: Boolean,
    val grantsPass: Boolean
)

enum class BictorysPaymentMethod(
    val id: String,
    val label: String,
    val queryParam: String?
) {
    HOSTED_CHECKOUT("checkout", "Checkout complet", null),
    ORANGE_MONEY("orange_money", "Orange Money", "orange_money"),
    WAVE("wave_money", "Wave", "wave_money"),
    CARD("card", "Carte bancaire", "card")
}

data class PendingPaymentRecord(
    val chargeId: String,
    val transactionId: String,
    val paymentReference: String,
    val packId: String,
    val packTitle: String,
    val amountXof: Int,
    val gemsReward: Int,
    val refillsHearts: Boolean,
    val grantsPass: Boolean,
    val checkoutUrl: String,
    val status: String,
    val createdAtMillis: Long,
    val credited: Boolean = false
)

enum class NormalizedPaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED
}

object BictorysValidator {
    val supportedCountries: List<Pair<String, String>> = listOf(
        "BF" to "Burkina Faso",
        "SN" to "Sénégal",
        "CI" to "Côte d'Ivoire",
        "ML" to "Mali",
        "NE" to "Niger",
        "TG" to "Togo",
        "BJ" to "Bénin"
    )

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun normalizePhone(raw: String): String {
        val compact = raw.trim().replace(Regex("[\\s\\-().]"), "")
        return compact
    }

    fun validateCustomerName(name: String): String? {
        val clean = name.trim()
        return when {
            clean.length < 2 -> "Saisis ton nom complet (au moins 2 caractères)."
            clean.length > 80 -> "Le nom ne doit pas dépasser 80 caractères."
            else -> null
        }
    }

    fun validatePhone(phone: String): String? {
        val normalized = normalizePhone(phone)
        val digitsOnly = normalized.removePrefix("+")
        return when {
            normalized.isEmpty() -> "Saisis ton numéro de téléphone pour le paiement."
            !digitsOnly.all { it.isDigit() } || digitsOnly.length !in 8..15 ->
                "Numéro invalide (ex. +22670000000 ou 70000000)."
            else -> null
        }
    }

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return null
        return if (emailRegex.matches(trimmed)) null
        else "Adresse email invalide."
    }

    fun isAllowedCountry(code: String): Boolean =
        supportedCountries.any { it.first.equals(code.trim(), ignoreCase = true) }

    fun isTrustedCheckoutUrl(rawUrl: String?): Boolean {
        val url = rawUrl?.trim().orEmpty()
        if (url.isEmpty()) return false
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host?.lowercase(Locale.ROOT)
            scheme == "https" &&
                host != null &&
                (host == "bictorys.com" || host.endsWith(".bictorys.com"))
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeStatus(rawStatus: String?): NormalizedPaymentStatus {
        val s = rawStatus?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            s in setOf("succeeded", "successful", "success", "succès", "completed", "paid", "approved") ->
                NormalizedPaymentStatus.SUCCEEDED
            s in setOf("failed", "failure", "échoué", "echoue", "canceled", "cancelled", "expired", "expiré", "expire", "rejected") ->
                NormalizedPaymentStatus.FAILED
            else -> NormalizedPaymentStatus.PENDING
        }
    }
}
