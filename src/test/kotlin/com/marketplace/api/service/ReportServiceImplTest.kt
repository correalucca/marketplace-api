package com.marketplace.api.service

import com.marketplace.api.entity.Order
import com.marketplace.api.entity.OrderItem
import com.marketplace.api.entity.Product
import com.marketplace.api.entity.User
import com.marketplace.api.entity.enums.OrderStatus
import com.marketplace.api.entity.enums.Role
import com.marketplace.api.exception.ResourceNotFoundException
import com.marketplace.api.repository.OrderItemRepository
import com.marketplace.api.repository.ProductRepository
import com.marketplace.api.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ReportServiceImplTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    private lateinit var reportService: ReportServiceImpl

    private lateinit var seller: User
    private lateinit var product1: Product
    private lateinit var product2: Product
    private lateinit var order: Order
    private lateinit var item1: OrderItem
    private lateinit var item2: OrderItem

    @BeforeEach
    fun setup() {
        reportService = ReportServiceImpl(userRepository, productRepository, orderItemRepository)

        seller = User(1L, "Vendedor", "vendedor@test.com", "123", null, Role.SELLER, null, null)

        product1 = Product(1L, "Produto A", "Desc A", BigDecimal("100.00"), 10, seller, 0, null, null)
        product2 = Product(2L, "Produto B", "Desc B", BigDecimal("50.00"), 20, seller, 0, null, null)

        order = Order(1L, seller, emptyList(), BigDecimal("150.00"), BigDecimal.ZERO, null, OrderStatus.DELIVERED, null, null)

        item1 = OrderItem(1L, order, product1, 1, BigDecimal("100.00"), BigDecimal("100.00"))
        item2 = OrderItem(2L, order, product2, 1, BigDecimal("50.00"), BigDecimal("50.00"))
    }

    @Test
    @DisplayName("Deve calcular resumo do vendedor corretamente")
    fun `should calculate seller report correctly`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(seller))
        `when`(productRepository.findBySeller(seller)).thenReturn(listOf(product1, product2))
        `when`(orderItemRepository.findByProductIn(anyList())).thenReturn(listOf(item1, item2))

        val result = reportService.getSellerReport(1L)

        assertEquals(1L, result.sellerId)
        assertEquals("Vendedor", result.sellerName)
        assertEquals(2, result.totalProducts)
        assertEquals(1L, result.totalOrders)
        assertEquals(BigDecimal("150.00"), result.totalSalesAmount)
        assertEquals(BigDecimal("150.00"), result.averageTicket)
    }

    @Test
    @DisplayName("Deve lancar ResourceNotFoundException quando vendedor nao existe")
    fun `should throw when seller not found`() {
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            reportService.getSellerReport(999L)
        }

        assertEquals("Seller not found with id: 999", exception.message)
    }

    @Test
    @DisplayName("Deve retornar averageTicket nulo quando nao houver vendas")
    fun `should return null average ticket when no sales`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(seller))
        `when`(productRepository.findBySeller(seller)).thenReturn(listOf(product1))
        `when`(orderItemRepository.findByProductIn(anyList())).thenReturn(emptyList())

        val result = reportService.getSellerReport(1L)

        assertEquals(0L, result.totalOrders)
        assertEquals(BigDecimal.ZERO, result.totalSalesAmount)
        assertNull(result.averageTicket)
    }

    @Test
    @DisplayName("Deve retornar dados mesmo quando vendedor nao tem produtos")
    fun `should return report when seller has no products`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(seller))
        `when`(productRepository.findBySeller(seller)).thenReturn(emptyList())
        `when`(orderItemRepository.findByProductIn(anyList())).thenReturn(emptyList())

        val result = reportService.getSellerReport(1L)

        assertEquals(0, result.totalProducts)
        assertEquals(0L, result.totalOrders)
        assertEquals(BigDecimal.ZERO, result.totalSalesAmount)
        assertNull(result.averageTicket)
    }

    @Test
    @DisplayName("Deve agregar pedidos de multiplos produtos em um unico total")
    fun `should aggregate multiple items from same order`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(seller))
        `when`(productRepository.findBySeller(seller)).thenReturn(listOf(product1, product2))
        `when`(orderItemRepository.findByProductIn(anyList())).thenReturn(listOf(item1, item2))

        val result = reportService.getSellerReport(1L)

        assertEquals(1L, result.totalOrders)
        assertEquals(BigDecimal("150.00"), result.totalSalesAmount)
    }
}
