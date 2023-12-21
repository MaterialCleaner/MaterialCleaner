package com.android.billingclient.api

data class ProductDetailsResult(
    val billingResult: BillingResult,
    val productDetailsList: List<ProductDetails>?
)
