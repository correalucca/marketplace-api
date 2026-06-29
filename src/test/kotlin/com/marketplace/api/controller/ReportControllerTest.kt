package com.marketplace.api.controller

import com.marketplace.api.dto.response.SellerReportResponse
import com.marketplace.api.service.ReportService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(ReportController::class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var reportService: ReportService

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve retornar 200 com dados do relatorio")
    fun `should return seller report`() {
        val response = SellerReportResponse(
            sellerId = 1L,
            sellerName = "Vendedor Teste",
            sellerEmail = "vendedor@test.com",
            totalProducts = 5,
            totalOrders = 10,
            totalSalesAmount = BigDecimal("5000.00"),
            averageTicket = BigDecimal("500.00")
        )

        `when`(reportService.getSellerReport(1L)).thenReturn(response)

        mockMvc.perform(get("/api/reports/seller/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sellerId").value(1L))
            .andExpect(jsonPath("$.sellerName").value("Vendedor Teste"))
            .andExpect(jsonPath("$.sellerEmail").value("vendedor@test.com"))
            .andExpect(jsonPath("$.totalProducts").value(5))
            .andExpect(jsonPath("$.totalOrders").value(10))
            .andExpect(jsonPath("$.totalSalesAmount").value(5000.00))
            .andExpect(jsonPath("$.averageTicket").value(500.00))
    }

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve retornar 200 quando averageTicket for nulo")
    fun `should return report with null average ticket`() {
        val response = SellerReportResponse(
            sellerId = 2L,
            sellerName = "Vendedor Sem Vendas",
            sellerEmail = "semvendas@test.com",
            totalProducts = 3,
            totalOrders = 0,
            totalSalesAmount = BigDecimal.ZERO,
            averageTicket = null
        )

        `when`(reportService.getSellerReport(2L)).thenReturn(response)

        mockMvc.perform(get("/api/reports/seller/2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sellerId").value(2L))
            .andExpect(jsonPath("$.totalOrders").value(0))
            .andExpect(jsonPath("$.totalSalesAmount").value(0))
            .andExpect(jsonPath("$.averageTicket").doesNotExist())
    }
}
