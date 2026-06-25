package com.marketplace.api.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketplace.api.dto.request.OrderItemRequest;
import com.marketplace.api.entity.Product;
import com.marketplace.api.exception.InsufficientStockException;
import com.marketplace.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("Deve lançar exceção quando estoque é insuficiente")
    void shouldThrowExceptionWhenStockIsInsufficient() {
        Product product = Product.builder()
                .id(1L)
                .name("Notebook")
                .stockQuantity(1)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        OrderItemRequest item = new OrderItemRequest(1L, 5);

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(item));
    }

    @Test
    @DisplayName("Deve reservar estoque com sucesso")
    void shouldReserveStockSuccessfully() {
        Product product = Product.builder()
                .id(1L)
                .name("Notebook")
                .stockQuantity(10)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        OrderItemRequest item = new OrderItemRequest(1L, 3);
        inventoryService.reserveStock(item);

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve liberar estoque com sucesso")
    void shouldReleaseStockSuccessfully() {
        Product product = Product.builder()
                .id(1L)
                .name("Notebook")
                .stockQuantity(5)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        OrderItemRequest item = new OrderItemRequest(1L, 2);
        inventoryService.releaseStock(item);

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve lançar exceção ao liberar estoque de produto inexistente")
    void shouldThrowWhenReleasingStockForNonExistentProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        OrderItemRequest item = new OrderItemRequest(99L, 1);

        assertThrows(com.marketplace.api.exception.ResourceNotFoundException.class,
                () -> inventoryService.releaseStock(item));
    }
}
