package com.android.billingclient.api

data class ConsumeResult(
    val billingResult: BillingResult,
    val purchaseToken: String?
)
