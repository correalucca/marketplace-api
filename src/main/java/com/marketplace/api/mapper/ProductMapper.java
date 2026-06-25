package com.marketplace.api.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.Product;

@Slf4j
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public Product toEntity(ProductRequest request) {
        log.debug("Mapping ProductRequest to entity: {}", request.getName());
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();
    }
}
