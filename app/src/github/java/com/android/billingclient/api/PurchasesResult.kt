package com.android.billingclient.api

data class PurchasesResult(
    val billingResult: BillingResult,
    val purchasesList: List<Purchase>
)
