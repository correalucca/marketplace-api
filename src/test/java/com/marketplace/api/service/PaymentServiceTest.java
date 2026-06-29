package com.marketplace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.marketplace.api.service.commission.CommissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketplace.api.dto.request.PaymentRequest;
import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.Order;
import com.marketplace.api.entity.Payment;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.OrderStatus;
import com.marketplace.api.entity.enums.PaymentStatus;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.common.OwnershipValidator;
import com.marketplace.api.exception.BusinessException;
import com.marketplace.api.exception.PaymentProcessingException;
import com.marketplace.api.exception.ResourceNotFoundException;
import com.marketplace.api.mapper.PaymentMapper;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private CommissionService commissionService;

    @Mock
    private OwnershipValidator ownershipValidator;

    @InjectMocks
    private PaymentService paymentService;

    private final User buyer = User.builder().id(1L).name("Buyer").email("buyer@test.com").role(Role.BUYER).build();
    private final User otherBuyer = User.builder().id(2L).name("Other").email("other@test.com").role(Role.BUYER).build();

    private final Order order = Order.builder()
            .id(1L).buyer(buyer).status(OrderStatus.PENDING).build();

    @Test
    @DisplayName("processPayment: deve lançar exceção quando pedido está cancelado")
    void shouldThrowExceptionWhenOrderIsCancelled() {
        Order cancelledOrder = Order.builder()
                .id(1L).buyer(buyer).status(OrderStatus.CANCELLED).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(cancelledOrder));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(100.00))
                .paymentMethod("CREDIT_CARD")
                .build();

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(request, buyer));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment: deve lançar exceção quando outro BUYER tenta pagar")
    void shouldThrowExceptionWhenOtherBuyerPays() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(100.00))
                .paymentMethod("CREDIT_CARD")
                .build();

        assertThrows(BusinessException.class, () -> paymentService.processPayment(request, otherBuyer));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment: deve lançar exceção quando pedido não existe")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentRequest request = PaymentRequest.builder()
                .orderId(99L)
                .amount(BigDecimal.valueOf(100.00))
                .paymentMethod("CREDIT_CARD")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> paymentService.processPayment(request, buyer));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByOrderId: deve lançar exceção quando outro BUYER tenta ver pagamento")
    void findByOrderIdShouldThrowWhenOtherBuyer() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> paymentService.findByOrderId(1L, otherBuyer));
    }

    @Test
    @DisplayName("findByOrderId: deve lançar exceção quando pedido não existe")
    void findByOrderIdShouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.findByOrderId(99L, buyer));
    }

    @Test
    @DisplayName("processPayment: deve processar pagamento com sucesso")
    void processPaymentShouldSucceed() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L).amount(BigDecimal.valueOf(100.00)).paymentMethod("CREDIT_CARD")
                .build();

        Payment payment = Payment.builder()
                .id(1L).order(order).amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.APPROVED).transactionId("tx-123")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L).orderId(1L).amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.APPROVED).transactionId("tx-123")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenReturn(payment);
        when(paymentMapper.toResponse(any())).thenReturn(response);

        PaymentResponse result = paymentService.processPayment(request, buyer);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("tx-123");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(commissionService).saveCommissions(order);
    }

    @Test
    @DisplayName("findByOrderId: deve retornar pagamento com sucesso")
    void findByOrderIdShouldSucceed() {
        Payment payment = Payment.builder()
                .id(1L).order(order).amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.APPROVED).transactionId("tx-123")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L).orderId(1L).amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.APPROVED).transactionId("tx-123")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.findByOrderId(1L, buyer);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("tx-123");
    }
}
