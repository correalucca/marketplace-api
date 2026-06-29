package com.marketplace.api.service

import com.marketplace.api.dto.response.SellerReportResponse
import com.marketplace.api.exception.ResourceNotFoundException
import com.marketplace.api.repository.OrderItemRepository
import com.marketplace.api.repository.ProductRepository
import com.marketplace.api.repository.UserRepository
import org.springframework.stereotype.Service

import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ReportServiceImpl(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val orderItemRepository: OrderItemRepository
) : ReportService {

    override fun getSellerReport(sellerId: Long): SellerReportResponse {
        val seller = userRepository.findById(sellerId)
            .orElseThrow { ResourceNotFoundException("Seller", sellerId) }

        val products = productRepository.findBySeller(seller)
        val items = orderItemRepository.findByProductIn(products)

        val totalOrders = items.map { it.order.id }.distinct().size.toLong()
        val totalSalesAmount = items.map { it.subtotal }.fold(BigDecimal.ZERO, BigDecimal::add)
        val averageTicket = if (totalOrders > 0) {
            totalSalesAmount.divide(totalOrders.toBigDecimal(), 2, RoundingMode.HALF_UP)
        } else {
            null
        }

        return SellerReportResponse(
            sellerId = seller.id,
            sellerName = seller.name,
            sellerEmail = seller.email,
            totalProducts = products.size,
            totalOrders = totalOrders,
            totalSalesAmount = totalSalesAmount,
            averageTicket = averageTicket
        )
    }
}
