package com.android.billingclient.api

suspend fun BillingClient.acknowledgePurchase(params: AcknowledgePurchaseParams): BillingResult {
    throw RuntimeException("Stub!")
}

suspend fun BillingClient.consumePurchase(params: ConsumeParams): ConsumeResult {
    throw RuntimeException("Stub!")
}

suspend fun BillingClient.queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult {
    throw RuntimeException("Stub!")
}

suspend fun BillingClient.queryPurchaseHistory(params: QueryPurchaseHistoryParams): PurchaseHistoryResult {
    throw RuntimeException("Stub!")
}

@Deprecated("")
suspend fun BillingClient.queryPurchaseHistory(skuType: String): PurchaseHistoryResult {
    throw RuntimeException("Stub!")
}

suspend fun BillingClient.queryPurchasesAsync(params: QueryPurchasesParams): PurchasesResult {
    throw RuntimeException("Stub!")
}

@Deprecated("")
suspend fun BillingClient.queryPurchasesAsync(skuType: String): PurchasesResult {
    throw RuntimeException("Stub!")
}

@Deprecated("")
suspend fun BillingClient.querySkuDetails(params: SkuDetailsParams): SkuDetailsResult {
    throw RuntimeException("Stub!")
}
