package com.android.billingclient.api

data class SkuDetailsResult(
    val billingResult: BillingResult,
    val skuDetailsList: List<SkuDetails>?
)
