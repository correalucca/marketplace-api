package com.marketplace.api.controller;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.api.config.resolver.CurrentUser;
import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.User;
import com.marketplace.api.service.ProductService;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            @CurrentUser User user) {
        log.debug("POST /api/products - name: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll(
            @RequestParam(required = false) String name) {
        log.debug("GET /api/products - name: {}", name);
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(productService.findByName(name));
        }
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        log.debug("GET /api/products/{}", id);
        return ResponseEntity.ok(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.debug("PUT /api/products/{}", id);
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("DELETE /api/products/{}", id);
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
