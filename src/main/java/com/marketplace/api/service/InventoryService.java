package com.marketplace.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.dto.request.OrderItemRequest;
import com.marketplace.api.entity.Product;
import com.marketplace.api.exception.InsufficientStockException;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class InventoryService {

    private final ProductRepository productRepository;

    @Autowired
    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void reserveStock(OrderItemRequest item) {
        log.debug("Reserving stock: productId={}, quantity={}", item.getProductId(), item.getQuantity());
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product not found for stock reservation: productId={}", item.getProductId());
                    return new ResourceNotFoundException("Product", item.getProductId());
                });

        if (product.getStockQuantity() < item.getQuantity()) {
            log.warn("Insufficient stock for product '{}': requested={}, available={}",
                    product.getName(), item.getQuantity(), product.getStockQuantity());
            throw new InsufficientStockException(
                    product.getName(),
                    item.getQuantity(),
                    product.getStockQuantity()
            );
        }

        product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
        productRepository.save(product);
        log.debug("Stock reserved: productId={}, remaining={}", product.getId(), product.getStockQuantity());
    }

    @Transactional
    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void releaseStock(OrderItemRequest item) {
        log.debug("Releasing stock: productId={}, quantity={}", item.getProductId(), item.getQuantity());
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> {
                    log.warn("Product not found for stock release: productId={}", item.getProductId());
                    return new ResourceNotFoundException("Product", item.getProductId());
                });

        product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        productRepository.save(product);
        log.debug("Stock released: productId={}, current={}", product.getId(), product.getStockQuantity());
    }
}
