package com.marketplace.api.service;

import java.util.List;
import java.util.stream.Collectors;

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

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final SecurityService securityService;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductMapper productMapper, SecurityService securityService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.securityService = securityService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request, User seller) {
        securityService.requireRole(Role.SELLER);

        Product product = productMapper.toEntity(request);
        product.setSeller(seller);

        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        securityService.requireRole(Role.SELLER);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        User current = securityService.getAuthenticatedUser();
        OwnershipValidator.validateOwnership(product.getSeller().getId(), current, "You can only update your own products");

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        securityService.requireRole(Role.SELLER);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        User current = securityService.getAuthenticatedUser();
        OwnershipValidator.validateOwnership(product.getSeller().getId(), current, "You can only delete your own products");

        productRepository.delete(product);
    }
}
