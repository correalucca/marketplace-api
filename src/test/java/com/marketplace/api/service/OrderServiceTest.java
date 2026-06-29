package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.marketplace.api.service.security.SecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketplace.api.dto.request.OrderItemRequest;
import com.marketplace.api.dto.request.OrderRequest;
import com.marketplace.api.dto.response.OrderResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.entity.enums.ShippingType;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.OrderMapper;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.ProductRepository;
import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.service.factory.ShippingStrategyFactory;
import com.marketplace.api.service.strategy.ShippingStrategy;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ShippingStrategyFactory shippingFactory;

    @Mock
    private ShippingStrategy shippingStrategy;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SecurityService securityService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private OrderService orderService;

    private final User buyer = User.builder().id(1L).name("Buyer").email("buyer@test.com").role(Role.BUYER).build();
    private final User otherBuyer = User.builder().id(2L).name("Other").email("other@test.com").role(Role.BUYER).build();
    private final User seller = User.builder().id(3L).name("Seller").email("seller@test.com").role(Role.SELLER).build();

    private final Product product = Product.builder()
            .id(1L).name("Notebook").price(BigDecimal.valueOf(3000.00)).stockQuantity(10)
            .seller(seller).build();

    private final OrderRequest request = OrderRequest.builder()
            .items(Collections.singletonList(new OrderItemRequest(1L, 2)))
            .shippingType("EXPRESS")
            .build();

    private final Order order = Order.builder()
            .id(1L).buyer(buyer).status(OrderStatus.PENDING)
            .totalAmount(BigDecimal.valueOf(6030.00))
            .shippingAmount(BigDecimal.valueOf(30.00))
            .shippingType(ShippingType.EXPRESS)
            .items(Collections.singletonList(
                OrderItem.builder().id(1L).product(product).quantity(2).build()))
            .build();

    private final OrderResponse response = OrderResponse.builder()
            .id(1L).buyerId(1L).buyerName("Buyer")
            .totalAmount(BigDecimal.valueOf(6030.00))
            .shippingAmount(BigDecimal.valueOf(30.00))
            .shippingType(ShippingType.EXPRESS)
            .status(OrderStatus.PENDING)
            .build();

    @Test
    @DisplayName("create: deve criar pedido com sucesso quando BUYER")
    void createShouldSucceedWhenBuyer() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(shippingFactory.getStrategy("EXPRESS")).thenReturn(shippingStrategy);
        when(shippingStrategy.calculate(any())).thenReturn(BigDecimal.valueOf(30.00));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderService.create(request, buyer);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(securityService).requireRole(Role.BUYER);
        verify(inventoryService).reserveStock(any());
        verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("create: deve lançar exceção quando produto não existe")
    void createShouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        OrderRequest req = OrderRequest.builder()
                .items(Collections.singletonList(new OrderItemRequest(99L, 2)))
                .shippingType("EXPRESS")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> orderService.create(req, buyer));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: deve lançar exceção quando SELLER tenta criar pedido (role BUYER necessária)")
    void createShouldThrowWhenSeller() {
        doThrow(new BusinessException("Access denied: BUYER role required"))
            .when(securityService).requireRole(Role.BUYER);

        assertThrows(BusinessException.class, () -> orderService.create(request, seller));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById: deve retornar pedido quando próprio BUYER")
    void findByIdShouldReturnOrderWhenOwnBuyer() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderService.findById(1L, buyer);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: deve lançar exceção quando outro BUYER tenta acessar")
    void findByIdShouldThrowWhenOtherBuyer() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.findById(1L, otherBuyer));
    }

    @Test
    @DisplayName("findById: deve retornar quando ADMIN acessa pedido de outro")
    void findByIdShouldReturnWhenAdmin() {
        User admin = User.builder().id(99L).role(Role.ADMIN).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse result = orderService.findById(1L, admin);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("findById: deve lançar exceção quando pedido não existe")
    void findByIdShouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(99L, buyer));
    }

    @Test
    @DisplayName("findByBuyer: deve retornar pedidos do comprador")
    void findByBuyerShouldReturnOrders() {
        when(orderRepository.findByBuyer(buyer)).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        List<OrderResponse> result = orderService.findByBuyer(buyer);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("cancel: deve cancelar pedido com sucesso quando próprio BUYER")
    void cancelShouldSucceedWhenOwnBuyer() {
        Order pendingOrder = Order.builder()
                .id(1L).buyer(buyer).status(OrderStatus.PENDING)
                .items(Collections.singletonList(
                    OrderItem.builder().id(1L).product(product).quantity(2).build()))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        orderService.cancel(1L, buyer);

        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).releaseStock(any());
        verify(orderRepository, times(1)).save(pendingOrder);
    }

    @Test
    @DisplayName("cancel: deve lançar exceção quando outro BUYER tenta cancelar")
    void cancelShouldThrowWhenOtherBuyer() {
        Order pendingOrder = Order.builder()
                .id(1L).buyer(buyer).status(OrderStatus.PENDING).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThrows(BusinessException.class, () -> orderService.cancel(1L, otherBuyer));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel: deve lançar exceção quando pedido não existe")
    void cancelShouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.cancel(99L, buyer));
    }

    @Test
    @DisplayName("cancel: deve lançar exceção quando pedido já foi entregue")
    void cancelShouldThrowWhenDelivered() {
        Order deliveredOrder = Order.builder()
                .id(1L).buyer(buyer).status(OrderStatus.DELIVERED).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(deliveredOrder));

        assertThrows(BusinessException.class, () -> orderService.cancel(1L, buyer));
        verify(orderRepository, never()).save(any());
    }
}
