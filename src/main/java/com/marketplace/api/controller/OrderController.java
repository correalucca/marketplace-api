package com.marketplace.api.controller;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.api.dto.request.OrderRequest;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.service.OrderService;
import com.marketplace.api.service.security.SecurityService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final SecurityService securityService;

    @Autowired
    public OrderController(OrderService orderService, SecurityService securityService) {
        this.orderService = orderService;
        this.securityService = securityService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        log.debug("POST /api/orders - items: {}", request.getItems().size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request, securityService.getAuthenticatedUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        log.debug("GET /api/orders/{}", id);
        return ResponseEntity.ok(orderService.findById(id, securityService.getAuthenticatedUser()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listMyOrders() {
        log.debug("GET /api/orders");
        return ResponseEntity.ok(orderService.findByBuyer(securityService.getAuthenticatedUser()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        log.debug("POST /api/orders/{}/cancel", id);
        orderService.cancel(id, securityService.getAuthenticatedUser());
        return ResponseEntity.noContent().build();
    }
}
