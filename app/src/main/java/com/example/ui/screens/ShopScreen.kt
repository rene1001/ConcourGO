package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.payment.BictorysPaymentMethod
import com.example.data.payment.BictorysValidator
import com.example.data.payment.PendingPaymentRecord
import com.example.data.payment.StorePack
import com.example.ui.components.Tactile3DButton
import com.example.ui.theme.LocalConcoursColors
import com.example.ui.viewmodel.MainDashboardState
import com.example.ui.viewmodel.ShopUiState

@Composable
fun ShopScreen(
    dashboardState: MainDashboardState,
    shopState: ShopUiState,
    onSelectPack: (String) -> Unit,
    onSelectMethod: (BictorysPaymentMethod) -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onPayWithBictorys: () -> Unit,
    onClearCheckoutUrl: () -> Unit,
    onVerifyPending: () -> Unit,
    onDismissPending: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extraColors = LocalConcoursColors.current
    val stats = dashboardState.userStats
    val hasPass = dashboardState.preferences.hasPremiumPass

    LaunchedEffect(shopState.checkoutUrlToOpen) {
        val url = shopState.checkoutUrlToOpen ?: return@LaunchedEffect
        openTrustedCheckoutUrl(context, url)
        onClearCheckoutUrl()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("shop_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
            item(key = "shop_header") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = extraColors.gemContainer,
                    border = BorderStroke(1.5.dp, extraColors.gem.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingBag,
                                    contentDescription = stringResource(R.string.shop_title),
                                    tint = extraColors.gem
                                )
                                Text(
                                    text = stringResource(R.string.shop_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = extraColors.onGemContainer
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (shopState.isSandboxMode) extraColors.goldContainer else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (shopState.isSandboxMode) {
                                        stringResource(R.string.shop_sandbox_badge)
                                    } else {
                                        stringResource(R.string.shop_live_badge)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (shopState.isSandboxMode) {
                                        extraColors.onGoldContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.shop_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.onGemContainer.copy(alpha = 0.85f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShopBalancePill(
                                icon = Icons.Rounded.Diamond,
                                label = "${stats.gems} gemmes",
                                tint = extraColors.gem,
                                bg = MaterialTheme.colorScheme.surface
                            )
                            ShopBalancePill(
                                icon = Icons.Rounded.Favorite,
                                label = "${stats.hearts}/5 cœurs",
                                tint = extraColors.heart,
                                bg = MaterialTheme.colorScheme.surface
                            )
                            ShopBalancePill(
                                icon = Icons.Rounded.WorkspacePremium,
                                label = if (hasPass) {
                                    stringResource(R.string.shop_pass_active)
                                } else {
                                    stringResource(R.string.shop_pass_inactive)
                                },
                                tint = if (hasPass) extraColors.goldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                bg = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }

            if (!shopState.isApiKeyConfigured) {
                item(key = "shop_missing_key_notice") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_missing_key_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = extraColors.goldContainer,
                        border = BorderStroke(1.dp, extraColors.gold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = extraColors.onGoldContainer
                            )
                            Text(
                                text = stringResource(R.string.shop_missing_key),
                                style = MaterialTheme.typography.bodySmall,
                                color = extraColors.onGoldContainer
                            )
                        }
                    }
                }
            }

            if (shopState.errorMessage != null) {
                item(key = "shop_error_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_error_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        onClick = onClearMessages
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = shopState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (shopState.successMessage != null) {
                item(key = "shop_success_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_success_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = onClearMessages
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = shopState.successMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            shopState.pendingPayment?.let { pending ->
                item(key = "shop_pending_card") {
                    PendingPaymentCard(
                        pending = pending,
                        isVerifying = shopState.isVerifying,
                        onOpenCheckout = {
                            openTrustedCheckoutUrl(context, pending.checkoutUrl)
                        },
                        onVerify = onVerifyPending,
                        onDismiss = onDismissPending
                    )
                }
            }

            if (dashboardState.preferences.lastPaymentReceipt.isNotBlank()) {
                item(key = "shop_last_receipt") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dernier achat validé : ${dashboardState.preferences.lastPaymentReceipt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Packs List
            item(key = "shop_packs_heading") {
                Text(
                    text = stringResource(R.string.shop_packs_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            shopState.packs.forEach { pack ->
                item(key = "pack_${pack.id}") {
                    StorePackCard(
                        pack = pack,
                        selected = pack.id == shopState.selectedPackId,
                        onClick = { onSelectPack(pack.id) }
                    )
                }
            }

            // Payment Method Selection
            item(key = "shop_methods") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shop_method_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BictorysPaymentMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = shopState.selectedMethod == method,
                                    onClick = { onSelectMethod(method) },
                                    label = { Text(method.label) },
                                    modifier = Modifier.testTag("shop_method_${method.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Candidate Details Form
            item(key = "shop_customer_form") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shop_customer_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = shopState.customerName,
                            onValueChange = onUpdateName,
                            label = { Text(stringResource(R.string.shop_field_name)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_input_name")
                        )

                        OutlinedTextField(
                            value = shopState.customerPhone,
                            onValueChange = onUpdatePhone,
                            label = { Text(stringResource(R.string.shop_field_phone)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_input_phone")
                        )

                        OutlinedTextField(
                            value = shopState.customerEmail,
                            onValueChange = onUpdateEmail,
                            label = { Text(stringResource(R.string.shop_field_email)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_input_email")
                        )

                        Text(
                            text = stringResource(R.string.shop_field_country),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BictorysValidator.supportedCountries.forEach { (code, label) ->
                                FilterChip(
                                    selected = shopState.countryCode == code,
                                    onClick = { onSelectCountry(code) },
                                    label = { Text("$code • $label") },
                                    modifier = Modifier.testTag("shop_country_$code")
                                )
                            }
                        }
                    }
                }
            }

            // Checkout CTA Button
            item(key = "shop_checkout_cta") {
                val selectedPack = shopState.selectedPack
                val amount = selectedPack?.amountXof ?: 0
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tactile3DButton(
                        text = if (shopState.isSubmitting) {
                            "Connexion à Bictorys..."
                        } else {
                            stringResource(R.string.shop_pay_button, amount)
                        },
                        enabled = selectedPack != null && !shopState.isSubmitting,
                        leadingIcon = Icons.Rounded.CreditCard,
                        containerColor = extraColors.gem,
                        shadowColor = extraColors.unitColors["cobalt"]?.second ?: extraColors.gem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shop_pay_button"),
                        onClick = onPayWithBictorys
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "Transaction chiffrée HTTPS via Bictorys",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "shop_bottom_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ShopBalancePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

@Composable
private fun StorePackCard(
    pack: StorePack,
    selected: Boolean,
    onClick: () -> Unit
) {
    val extraColors = LocalConcoursColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("shop_pack_${pack.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) extraColors.gemContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) extraColors.gem else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (pack.grantsPass) extraColors.goldContainer else extraColors.gemContainer
            ) {
                Icon(
                    imageVector = if (pack.grantsPass) Icons.Rounded.WorkspacePremium else Icons.Rounded.Diamond,
                    contentDescription = pack.title,
                    tint = if (pack.grantsPass) extraColors.goldDark else extraColors.gem,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (pack.badge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = extraColors.goldContainer
                        ) {
                            Text(
                                text = pack.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = extraColors.onGoldContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = pack.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) extraColors.gem else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${pack.amountXof} XOF",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PendingPaymentCard(
    pending: PendingPaymentRecord,
    isVerifying: Boolean,
    onOpenCheckout: () -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit
) {
    val extraColors = LocalConcoursColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_pending_card"),
        shape = RoundedCornerShape(20.dp),
        color = extraColors.goldContainer,
        border = BorderStroke(1.5.dp, extraColors.gold)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.shop_pending_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = extraColors.onGoldContainer
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("shop_clear_pending_button")
                ) {
                    Text(stringResource(R.string.shop_cancel_pending))
                }
            }
            Text(
                text = "${pending.packTitle} • ${stringResource(R.string.shop_pending_ref, pending.paymentReference, pending.amountXof)}",
                style = MaterialTheme.typography.bodySmall,
                color = extraColors.onGoldContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (BictorysValidator.isTrustedCheckoutUrl(pending.checkoutUrl)) {
                    OutlinedButton(
                        onClick = onOpenCheckout,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_open_checkout_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.shop_open_checkout))
                    }
                }
                Button(
                    onClick = onVerify,
                    enabled = !isVerifying,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_verify_payment_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.shop_verify_status))
                }
            }
        }
    }
}

private fun openTrustedCheckoutUrl(context: Context, rawUrl: String) {
    if (!BictorysValidator.isTrustedCheckoutUrl(rawUrl)) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No browser installed to open checkout URL
    }
}
