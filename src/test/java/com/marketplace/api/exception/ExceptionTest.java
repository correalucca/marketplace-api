package com.marketplace.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceptionTest {

    @Test
    @DisplayName("BusinessException: deve armazenar mensagem")
    void businessExceptionShouldStoreMessage() {
        BusinessException ex = new BusinessException("Email already registered");
        assertThat(ex.getMessage()).isEqualTo("Email already registered");
    }

    @Test
    @DisplayName("ResourceNotFoundException: deve formatar mensagem com resource e id")
    void resourceNotFoundExceptionShouldFormatMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product", 1L);
        assertThat(ex.getMessage()).isEqualTo("Product not found with id: 1");
    }

    @Test
    @DisplayName("ResourceNotFoundException: deve aceitar mensagem direta")
    void resourceNotFoundExceptionShouldAcceptDirectMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Custom error");
        assertThat(ex.getMessage()).isEqualTo("Custom error");
    }

    @Test
    @DisplayName("InsufficientStockException: deve formatar mensagem")
    void insufficientStockExceptionShouldFormatMessage() {
        InsufficientStockException ex = new InsufficientStockException("Notebook", 5, 2);
        assertThat(ex.getMessage()).contains("Notebook").contains("5").contains("2");
    }

    @Test
    @DisplayName("PaymentProcessingException: deve armazenar mensagem")
    void paymentProcessingExceptionShouldStoreMessage() {
        PaymentProcessingException ex = new PaymentProcessingException("Payment failed");
        assertThat(ex.getMessage()).isEqualTo("Payment failed");
    }

    @Test
    @DisplayName("PaymentProcessingException: deve armazenar causa")
    void paymentProcessingExceptionShouldStoreCause() {
        Throwable cause = new RuntimeException("Timeout");
        PaymentProcessingException ex = new PaymentProcessingException("Payment failed", cause);
        assertThat(ex.getMessage()).isEqualTo("Payment failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
