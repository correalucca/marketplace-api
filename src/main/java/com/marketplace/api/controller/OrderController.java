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

import com.marketplace.api.config.resolver.CurrentUser;
import com.marketplace.api.dto.request.OrderRequest;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.service.OrderService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request, @CurrentUser User user) {
        log.debug("POST /api/orders - items: {}", request.getItems().size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id, @CurrentUser User user) {
        log.debug("GET /api/orders/{}", id);
        return ResponseEntity.ok(orderService.findById(id, user));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listMyOrders(@CurrentUser User user) {
        log.debug("GET /api/orders");
        return ResponseEntity.ok(orderService.findByBuyer(user));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @CurrentUser User user) {
        log.debug("POST /api/orders/{}/cancel", id);
        orderService.cancel(id, user);
        return ResponseEntity.noContent().build();
    }
}
