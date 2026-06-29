package com.marketplace.api.service

import com.marketplace.api.dto.response.SellerReportResponse

interface ReportService {
    fun getSellerReport(sellerId: Long): SellerReportResponse
}
