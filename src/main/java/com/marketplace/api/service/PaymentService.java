package com.marketplace.api.service;

import java.util.UUID;

import com.marketplace.api.service.commission.CommissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.dto.request.PaymentRequest;
import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.Payment;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.PaymentStatus;
import com.marketplace.api.exception.PaymentProcessingException;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.PaymentMapper;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final CommissionService commissionService;
    private final OwnershipValidator ownershipValidator;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, PaymentMapper paymentMapper, CommissionService commissionService, OwnershipValidator ownershipValidator) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
        this.commissionService = commissionService;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, User currentUser) {
        log.info("Processing payment for orderId={}, amount={}, method={}",
                request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    log.warn("Order not found for payment: orderId={}", request.getOrderId());
                    return new ResourceNotFoundException("Order", request.getOrderId());
                });

        ownershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only pay for your own orders");

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Payment rejected - order is cancelled: orderId={}", request.getOrderId());
            throw new PaymentProcessingException("Cannot process payment for cancelled order");
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .status(PaymentStatus.APPROVED)
                .paymentMethod(request.getPaymentMethod())
                .transactionId(UUID.randomUUID().toString())
                .build();

        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        commissionService.saveCommissions(order);

        log.info("Payment processed successfully: transactionId={}, orderId={}, status={}",
                payment.getTransactionId(), request.getOrderId(), payment.getStatus());

        return paymentMapper.toResponse(payment);
    }

    public PaymentResponse findByOrderId(Long orderId, User currentUser) {
        log.debug("Finding payment for orderId={}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found when fetching payment: orderId={}", orderId);
                    return new ResourceNotFoundException("Order", orderId);
                });

        ownershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only view payments for your own orders");

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> {
                    log.warn("Payment not found for orderId={}", orderId);
                    return new ResourceNotFoundException("Payment for order", orderId);
                });
        return paymentMapper.toResponse(payment);
    }
}
