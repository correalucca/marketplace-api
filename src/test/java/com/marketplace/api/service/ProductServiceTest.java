package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.marketplace.api.service.security.SecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.ProductMapper;
import com.marketplace.api.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SecurityService securityService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private ProductService productService;

    private final User seller = User.builder().id(1L).name("Seller").email("seller@test.com").role(Role.SELLER).build();
    private final User otherSeller = User.builder().id(2L).name("Other").email("other@test.com").role(Role.SELLER).build();
    private final User buyer = User.builder().id(3L).name("Buyer").email("buyer@test.com").role(Role.BUYER).build();

    private final ProductRequest request = ProductRequest.builder()
            .name("Product").description("Desc").price(BigDecimal.TEN).stockQuantity(10).build();

    private final Product product = Product.builder()
            .id(1L).name("Product").description("Desc").price(BigDecimal.TEN).stockQuantity(10).seller(seller).build();

    private final ProductResponse response = ProductResponse.builder()
            .id(1L).name("Product").description("Desc").price(BigDecimal.TEN).stockQuantity(10)
            .sellerId(1L).sellerName("Seller").build();

    @Test
    @DisplayName("create: deve criar produto quando SELLER")
    void createShouldSucceedWhenSeller() {
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.create(request, seller);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(product.getSeller()).isEqualTo(seller);
        verify(securityService).requireRole(Role.SELLER);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("create: deve lançar exceção quando BUYER")
    void createShouldThrowWhenBuyer() {
        doThrow(new BusinessException("Access denied: SELLER role required"))
            .when(securityService).requireRole(Role.SELLER);

        assertThrows(BusinessException.class, () -> productService.create(request, buyer));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById: deve retornar produto quando existe")
    void findByIdShouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: deve lançar exceção quando não existe")
    void findByIdShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(99L));
    }

    @Test
    @DisplayName("findAll: deve retornar lista de produtos")
    void findAllShouldReturnList() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        List<ProductResponse> result = productService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByName: deve retornar produtos filtrados")
    void findByNameShouldReturnFiltered() {
        when(productRepository.findByNameContainingIgnoreCase("prod")).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        List<ProductResponse> result = productService.findByName("prod");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("update: deve atualizar quando próprio SELLER")
    void updateShouldSucceedWhenOwnSeller() {
        when(securityService.getAuthenticatedUser()).thenReturn(seller);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isNotNull();
        verify(securityService).requireRole(Role.SELLER);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("update: deve lançar exceção quando outro SELLER tenta atualizar")
    void updateShouldThrowWhenOtherSeller() {
        when(securityService.getAuthenticatedUser()).thenReturn(otherSeller);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> productService.update(1L, request));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: deve lançar exceção quando produto não existe")
    void updateShouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(99L, request));
    }

    @Test
    @DisplayName("delete: deve deletar quando próprio SELLER")
    void deleteShouldSucceedWhenOwnSeller() {
        when(securityService.getAuthenticatedUser()).thenReturn(seller);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("delete: deve lançar exceção quando outro SELLER tenta deletar")
    void deleteShouldThrowWhenOtherSeller() {
        when(securityService.getAuthenticatedUser()).thenReturn(otherSeller);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> productService.delete(1L));
        verify(productRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: deve lançar exceção quando produto não existe")
    void deleteShouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.delete(99L));
    }
}
