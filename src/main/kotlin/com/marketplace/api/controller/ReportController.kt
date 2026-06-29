package com.marketplace.api.controller

import com.marketplace.api.dto.response.SellerReportResponse
import com.marketplace.api.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
class ReportController(private val reportService: ReportService) {

    @GetMapping("/seller/{sellerId}")
    fun getSellerReport(@PathVariable sellerId: Long): ResponseEntity<SellerReportResponse> {
        return ResponseEntity.ok(reportService.getSellerReport(sellerId))
    }
}
