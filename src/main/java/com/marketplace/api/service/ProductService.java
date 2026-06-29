package com.marketplace.api.service;

import java.util.List;
import java.util.stream.Collectors;

import com.marketplace.api.service.security.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.dto.request.ProductRequest;
import com.marketplace.api.dto.response.ProductResponse;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.ProductMapper;
import com.marketplace.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final SecurityService securityService;
    private final OwnershipValidator ownershipValidator;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductMapper productMapper, SecurityService securityService, OwnershipValidator ownershipValidator) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.securityService = securityService;
        this.ownershipValidator = ownershipValidator;
    }

    @Transactional
    public ProductResponse create(ProductRequest request, User seller) {
        securityService.requireRole(Role.SELLER);

        Product product = productMapper.toEntity(request);
        product.setSeller(seller);

        ProductResponse response = productMapper.toResponse(productRepository.save(product));
        log.info("Product created: id={}, name={}, sellerId={}", response.getId(), response.getName(), seller.getId());
        return response;
    }

    public ProductResponse findById(Long id) {
        log.debug("Finding product by id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found: id={}", id);
                    return new ResourceNotFoundException("Product", id);
                });
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> findAll() {
        log.debug("Listing all products");
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByName(String name) {
        log.debug("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Updating product id={}", id);
        securityService.requireRole(Role.SELLER);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for update: id={}", id);
                    return new ResourceNotFoundException("Product", id);
                });

        User current = securityService.getAuthenticatedUser();
        ownershipValidator.validateOwnership(product.getSeller().getId(), current, "You can only update your own products");

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        ProductResponse response = productMapper.toResponse(productRepository.save(product));
        log.info("Product updated: id={}", id);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting product id={}", id);
        securityService.requireRole(Role.SELLER);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for deletion: id={}", id);
                    return new ResourceNotFoundException("Product", id);
                });

        User current = securityService.getAuthenticatedUser();
        ownershipValidator.validateOwnership(product.getSeller().getId(), current, "You can only delete your own products");

        productRepository.delete(product);
        log.info("Product deleted: id={}", id);
    }
}
