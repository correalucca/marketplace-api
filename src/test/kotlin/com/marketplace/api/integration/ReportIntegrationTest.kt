package com.marketplace.api.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal

class ReportIntegrationTest : AbstractReportIntegrationTest() {

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve retornar resumo completo")
    fun `should return full seller report`() {
        val seller = createSeller("Maria Vendedora", "maria@test.com")
        val buyer = createBuyer("Joao Comprador")
        val notebook = createProduct(seller, "Notebook", BigDecimal("3000.00"))
        val mouse = createProduct(seller, "Mouse", BigDecimal("150.00"))
        createOrderWithItems(buyer, notebook to 1, mouse to 1)

        val response = restTemplate.getForEntity(reportUrl(seller.id!!), String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val json = objectMapper.readTree(response.body)
        assertEquals(seller.id, json["sellerId"].asLong())
        assertEquals("Maria Vendedora", json["sellerName"].asText())
        assertEquals("maria@test.com", json["sellerEmail"].asText())
        assertEquals(2, json["totalProducts"].asInt())
        assertEquals(1, json["totalOrders"].asLong())
        assertEquals(0, BigDecimal("3150.00").compareTo(json["totalSalesAmount"].decimalValue()))
        assertEquals(0, BigDecimal("3150.00").compareTo(json["averageTicket"].decimalValue()))

        println("=== RELATORIO COMPLETO === $response.body")
    }

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve retornar 404 para vendedor inexistente")
    fun `should return 404 when seller does not exist`() {
        val response = restTemplate.getForEntity(reportUrl(999L), String::class.java)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        println("=== VENDEDOR NAO ENCONTRADO === $response.body")
    }

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve retornar vendas zeradas")
    fun `should return zero sales for new seller`() {
        val seller = createSeller("Novo Vendedor")
        createProduct(seller, "Produto Sem Vendas", BigDecimal("100.00"))

        val response = restTemplate.getForEntity(reportUrl(seller.id!!), String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val json = objectMapper.readTree(response.body)
        assertEquals(1, json["totalProducts"].asInt())
        assertEquals(0, json["totalOrders"].asLong())
        assertEquals(0, BigDecimal.ZERO.compareTo(json["totalSalesAmount"].decimalValue()))
        assertTrue(json["averageTicket"].isNull)

        println("=== VENDAS ZERADAS === $response.body")
    }

    @Test
    @DisplayName("GET /api/reports/seller/{id} - Deve agregar multiplos pedidos")
    fun `should aggregate multiple orders`() {
        val seller = createSeller("Vendedora Top")
        val buyer = createBuyer("Cliente Fiel")
        val produto = createProduct(seller, "Curso Java", BigDecimal("500.00"))
        createOrderWithItems(buyer, produto to 1)
        createOrderWithItems(buyer, produto to 2)

        val response = restTemplate.getForEntity(reportUrl(seller.id!!), String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val json = objectMapper.readTree(response.body)
        assertEquals(2, json["totalOrders"].asLong())
        assertEquals(0, BigDecimal("1500.00").compareTo(json["totalSalesAmount"].decimalValue()))
        assertEquals(0, BigDecimal("750.00").compareTo(json["averageTicket"].decimalValue()))

        println("=== MULTIPLOS PEDIDOS === $response.body")
    }
}
