package com.marketplace.api.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.marketplace.api.dto.response.PaymentResponse;
import com.marketplace.api.entity.Payment;

@Slf4j
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        log.debug("Mapping Payment to response: id={}, transactionId={}", payment.getId(), payment.getTransactionId());
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
