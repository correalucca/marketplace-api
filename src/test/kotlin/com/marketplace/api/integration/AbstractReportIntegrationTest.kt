package com.marketplace.api.integration

import com.marketplace.api.entity.Order
import com.marketplace.api.entity.OrderItem
import com.marketplace.api.entity.Product
import com.marketplace.api.entity.User
import com.marketplace.api.entity.enums.OrderStatus
import com.marketplace.api.entity.enums.Role
import com.marketplace.api.repository.OrderItemRepository
import com.marketplace.api.repository.OrderRepository
import com.marketplace.api.repository.ProductRepository
import com.marketplace.api.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractReportIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var productRepository: ProductRepository

    @Autowired
    protected lateinit var orderRepository: OrderRepository

    @Autowired
    protected lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanupDatabase() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()
        userRepository.deleteAll()
    }

    protected fun createSeller(name: String, email: String = name.lowercase().replace(" ", ".") + "@test.com"): User =
        userRepository.save(User(null, name, email, "123", null, Role.SELLER, null, null))

    protected fun createBuyer(name: String, email: String = name.lowercase().replace(" ", ".") + "@test.com"): User =
        userRepository.save(User(null, name, email, "123", null, Role.BUYER, null, null))

    protected fun createProduct(seller: User, name: String, price: BigDecimal, stock: Int = 10): Product =
        productRepository.save(Product(null, name, "Desc $name", price, stock, seller, 0, null, null))

    protected fun createOrderWithItems(
        buyer: User,
        vararg items: Pair<Product, Int>
    ): Order {
        val subtotal = items.sumOf { (product, qty) -> product.price.multiply(BigDecimal(qty)).toDouble() }
        val order = orderRepository.save(
            Order(null, buyer, mutableListOf(), BigDecimal.valueOf(subtotal), BigDecimal.ZERO, null, OrderStatus.DELIVERED, null, null)
        )
        items.forEach { (product, qty) ->
            orderItemRepository.save(OrderItem(null, order, product, qty, product.price, product.price.multiply(BigDecimal(qty))))
        }
        return order
    }

    protected fun reportUrl(sellerId: Long): String =
        "http://localhost:$port/api/reports/seller/$sellerId"
}
