package com.marketplace.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.marketplace.api.service.security.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.dto.request.OrderItemRequest;
import com.marketplace.api.dto.request.OrderRequest;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.OrderMapper;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.ProductRepository;
import com.marketplace.api.service.factory.ShippingStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ShippingStrategyFactory shippingStrategyFactory;
    private final OrderMapper orderMapper;
    private final SecurityService securityService;
    private final OwnershipValidator ownershipValidator;

    @Autowired
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, InventoryService inventoryService, ShippingStrategyFactory shippingStrategyFactory, OrderMapper orderMapper, SecurityService securityService, OwnershipValidator ownershipValidator) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.shippingStrategyFactory = shippingStrategyFactory;
        this.orderMapper = orderMapper;
        this.securityService = securityService;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public OrderResponse create(OrderRequest request, User buyer) {
        log.info("Creating order for buyerId={} with {} items", buyer.getId(), request.getItems().size());
        securityService.requireRole(Role.BUYER);

        Order order = new Order();
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> {
                        log.warn("Product not found for order item: productId={}", itemReq.getProductId());
                        return new ResourceNotFoundException("Product", itemReq.getProductId());
                    });

            inventoryService.reserveStock(itemReq);

            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(itemSubtotal)
                    .build();

            order.getItems().add(orderItem);
            subtotal = subtotal.add(itemSubtotal);
        }

        order.setTotalAmount(subtotal);

        BigDecimal shippingAmount = shippingStrategyFactory.getStrategy(request.getShippingType()).calculate(order);
        order.setShippingAmount(shippingAmount);
        order.setTotalAmount(subtotal.add(shippingAmount));

        order = orderRepository.save(order);
        log.info("Order created: id={}, buyerId={}, total={}, status={}", order.getId(), buyer.getId(), order.getTotalAmount(), order.getStatus());
        return orderMapper.toResponse(order);
    }

    public OrderResponse findById(Long id, User currentUser) {
        log.debug("Finding order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found: id={}", id);
                    return new ResourceNotFoundException("Order", id);
                });
        ownershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only view your own orders");
        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> findByBuyer(User buyer) {
        log.debug("Finding orders for buyerId={}", buyer.getId());
        return orderRepository.findByBuyer(buyer).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancel(Long id, User currentUser) {
        log.info("Cancelling order id={} by userId={}", id, currentUser.getId());
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for cancellation: id={}", id);
                    return new ResourceNotFoundException("Order", id);
                });

        ownershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only cancel your own orders");

        if (order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("Cannot cancel delivered order id={}", id);
            throw new BusinessException("Cannot cancel a delivered order");
        }

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(new OrderItemRequest(item.getProduct().getId(), item.getQuantity()));
        }

        orderRepository.save(order);
        log.info("Order cancelled: id={}", id);
    }
}
