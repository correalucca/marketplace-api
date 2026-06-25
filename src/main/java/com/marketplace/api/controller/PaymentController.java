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

import com.marketplace.api.dto.request.PaymentRequest;
import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.service.PaymentService;
import com.marketplace.api.service.security.SecurityService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final SecurityService securityService;

    @Autowired
    public PaymentController(PaymentService paymentService, SecurityService securityService) {
        this.paymentService = paymentService;
        this.securityService = securityService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.debug("POST /api/payments - orderId: {}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request, securityService.getAuthenticatedUser()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> findByOrderId(@PathVariable Long orderId) {
        log.debug("GET /api/payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.findByOrderId(orderId, securityService.getAuthenticatedUser()));
    }
}
