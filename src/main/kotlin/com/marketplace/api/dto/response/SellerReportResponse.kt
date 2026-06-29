package com.marketplace.api.dto.response

import java.math.BigDecimal

data class SellerReportResponse(
    val sellerId: Long,
    val sellerName: String,
    val sellerEmail: String,
    val totalProducts: Int,
    val totalOrders: Long,
    val totalSalesAmount: BigDecimal,
    val averageTicket: BigDecimal?
)
