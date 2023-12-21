package com.android.billingclient.api

data class PurchaseHistoryResult(
    val billingResult: BillingResult,
    val purchaseHistoryRecordList: List<PurchaseHistoryRecord>?
)
