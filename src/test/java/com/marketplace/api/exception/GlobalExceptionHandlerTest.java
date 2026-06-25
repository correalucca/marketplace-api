package com.marketplace.api.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/test")
    static class TestController {
        @GetMapping("/resource-not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Product", 1L);
        }

        @GetMapping("/business-error")
        public void throwBusiness() {
            throw new BusinessException("Business error");
        }

        @GetMapping("/insufficient-stock")
        public void throwInsufficientStock() {
            throw new InsufficientStockException("Notebook", 5, 2);
        }

        @GetMapping("/payment-error")
        public void throwPaymentError() {
            throw new PaymentProcessingException("Payment failed");
        }

        @GetMapping("/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid argument");
        }

        @GetMapping("/data-integrity-error")
        public void throwDataIntegrity() {
            throw new DataIntegrityViolationException("Constraint violation");
        }

        @GetMapping("/general-error")
        public void throwGeneral() {
            throw new RuntimeException("Unexpected error");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve retornar 404 para ResourceNotFoundException")
    void shouldReturn404ForResourceNotFound() throws Exception {
        mockMvc.perform(get("/api/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Deve retornar 422 para BusinessException")
    void shouldReturn422ForBusinessException() throws Exception {
        mockMvc.perform(get("/api/test/business-error"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("Deve retornar 409 para InsufficientStockException")
    void shouldReturn409ForInsufficientStock() throws Exception {
        mockMvc.perform(get("/api/test/insufficient-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Deve retornar 402 para PaymentProcessingException")
    void shouldReturn402ForPaymentError() throws Exception {
        mockMvc.perform(get("/api/test/payment-error"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));
    }

    @Test
    @DisplayName("Deve retornar 400 para IllegalArgumentException")
    void shouldReturn400ForIllegalArgument() throws Exception {
        mockMvc.perform(get("/api/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve retornar 409 para DataIntegrityViolation")
    void shouldReturn409ForDataIntegrity() throws Exception {
        mockMvc.perform(get("/api/test/data-integrity-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Deve retornar 500 para erro genérico")
    void shouldReturn500ForGeneralError() throws Exception {
        mockMvc.perform(get("/api/test/general-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
