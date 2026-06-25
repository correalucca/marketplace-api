package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.marketplace.api.entity.Commission;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.OrderItem;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.repository.CommissionRepository;
import com.marketplace.api.service.factory.CommissionStrategyFactory;
import com.marketplace.api.service.commission.CommissionServiceImpl;
import com.marketplace.api.service.strategy.CommissionStrategy;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionStrategyFactory commissionStrategyFactory;

    @Mock
    private CommissionStrategy commissionStrategy;

    @InjectMocks
    private CommissionServiceImpl commissionService;

    private final User seller1 = User.builder().id(1L).name("Seller1").role(Role.SELLER).build();
    private final User seller2 = User.builder().id(2L).name("Seller2").role(Role.SELLER).build();

    private final Product product1 = Product.builder().id(1L).name("Product1").price(BigDecimal.valueOf(100)).seller(seller1).build();
    private final Product product2 = Product.builder().id(2L).name("Product2").price(BigDecimal.valueOf(200)).seller(seller2).build();

    @Test
    @DisplayName("saveCommissions: deve salvar comissão para um vendedor")
    void shouldSaveCommissionForSingleSeller() {
        Order order = Order.builder()
                .id(1L).status(OrderStatus.CONFIRMED)
                .items(List.of(
                    OrderItem.builder().id(1L).product(product1).quantity(1).unitPrice(BigDecimal.valueOf(100)).subtotal(BigDecimal.valueOf(100)).build(),
                    OrderItem.builder().id(2L).product(product1).quantity(2).unitPrice(BigDecimal.valueOf(100)).subtotal(BigDecimal.valueOf(200)).build()
                ))
                .totalAmount(BigDecimal.valueOf(300))
                .build();

        when(commissionStrategyFactory.getStrategy("STANDARD")).thenReturn(commissionStrategy);
        when(commissionStrategy.calculate(order)).thenReturn(BigDecimal.valueOf(30));

        ReflectionTestUtils.setField(commissionService, "commissionType", "STANDARD");

        commissionService.saveCommissions(order);

        verify(commissionRepository).save(any(Commission.class));
    }

    @Test
    @DisplayName("saveCommissions: deve salvar comissão proporcional para múltiplos vendedores")
    void shouldSaveProportionalCommissionForMultipleSellers() {
        Order order = Order.builder()
                .id(1L).status(OrderStatus.CONFIRMED)
                .items(List.of(
                    OrderItem.builder().id(1L).product(product1).quantity(1).unitPrice(BigDecimal.valueOf(100)).subtotal(BigDecimal.valueOf(100)).build(),
                    OrderItem.builder().id(2L).product(product2).quantity(1).unitPrice(BigDecimal.valueOf(200)).subtotal(BigDecimal.valueOf(200)).build()
                ))
                .totalAmount(BigDecimal.valueOf(300))
                .build();

        when(commissionStrategyFactory.getStrategy("STANDARD")).thenReturn(commissionStrategy);
        when(commissionStrategy.calculate(order)).thenReturn(BigDecimal.valueOf(30));

        ReflectionTestUtils.setField(commissionService, "commissionType", "STANDARD");

        commissionService.saveCommissions(order);

        verify(commissionRepository, times(2)).save(any(Commission.class));
    }

    @Test
    @DisplayName("findBySeller: deve retornar comissões do vendedor")
    void shouldReturnCommissionsBySeller() {
        Commission commission = Commission.builder().id(1L).seller(seller1).amount(BigDecimal.TEN).build();
        when(commissionRepository.findBySeller(seller1)).thenReturn(List.of(commission));

        List<Commission> result = commissionService.findBySeller(seller1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
