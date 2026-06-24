package com.marketplace.api.service;

import java.util.UUID;

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
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.PaymentMapper;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final CommissionService commissionService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, PaymentMapper paymentMapper, CommissionService commissionService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
        this.commissionService = commissionService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, User currentUser) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));

        OwnershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only pay for your own orders");

        if (order.getStatus() == OrderStatus.CANCELLED) {
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

        return paymentMapper.toResponse(payment);
    }

    public PaymentResponse findByOrderId(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OwnershipValidator.validateOwnership(order.getBuyer().getId(), currentUser, "You can only view payments for your own orders");

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order", orderId));
        return paymentMapper.toResponse(payment);
    }
}
