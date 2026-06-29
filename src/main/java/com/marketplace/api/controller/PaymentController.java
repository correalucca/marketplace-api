package com.marketplace.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.api.config.resolver.CurrentUser;
import com.marketplace.api.dto.request.PaymentRequest;
import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.service.PaymentService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request, @CurrentUser User user) {
        log.debug("POST /api/payments - orderId: {}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request, user));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> findByOrderId(@PathVariable Long orderId, @CurrentUser User user) {
        log.debug("GET /api/payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.findByOrderId(orderId, user));
    }
}
