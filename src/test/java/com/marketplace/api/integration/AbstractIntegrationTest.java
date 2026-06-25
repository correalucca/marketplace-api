package com.marketplace.api.integration;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.repository.CommissionRepository;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.PaymentRepository;
import com.marketplace.api.repository.ProductRepository;
import com.marketplace.api.repository.RefreshTokenRepository;
import com.marketplace.api.repository.UserRepository;
import com.marketplace.api.service.security.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "api.security.jwt.secret=test-jwt-secret-key-not-for-production-but-longer-for-testing",
    "api.security.jwt.access-expiration=900000",
    "api.security.jwt.refresh-expiration=604800000"
})
public abstract class AbstractIntegrationTest {
    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ProductRepository productRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected PaymentRepository paymentRepository;
    @Autowired
    protected CommissionRepository commissionRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        SecurityContextHolder.clearContext();
        paymentRepository.deleteAll();
        commissionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected String tokenFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    protected User createUser(String name, String email, Role role) {
        return userRepository.save(User.builder()
            .name(name).email(email)
            .password(passwordEncoder.encode("123456"))
            .role(role).build());
    }

    protected Product createProduct(User seller, String name, BigDecimal price, int stock) {
        return productRepository.save(Product.builder()
            .name(name).description("Desc")
            .price(price).stockQuantity(stock)
            .seller(seller).build());
    }

    protected Long extractId(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).get("id").asLong();
    }

    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected HttpHeaders authHeaders(User user) {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", tokenFor(user));
        return headers;
    }

    protected ResponseEntity<String> post(String path, String json, HttpHeaders headers) {
        return restTemplate.exchange(baseUrl() + path, HttpMethod.POST,
            new HttpEntity<>(json, headers), String.class);
    }

    protected ResponseEntity<String> get(String path, HttpHeaders headers) {
        return restTemplate.exchange(baseUrl() + path, HttpMethod.GET,
            new HttpEntity<>(headers), String.class);
    }

    protected ResponseEntity<String> put(String path, String json, HttpHeaders headers) {
        return restTemplate.exchange(baseUrl() + path, HttpMethod.PUT,
            new HttpEntity<>(json, headers), String.class);
    }

    protected ResponseEntity<String> delete(String path, HttpHeaders headers) {
        return restTemplate.exchange(baseUrl() + path, HttpMethod.DELETE,
            new HttpEntity<>(headers), String.class);
    }

    protected static final String PRODUCT_JSON = """
        {"name":"%s","description":"%s","price":%s,"stockQuantity":%d}
        """;

    protected static final String ORDER_JSON = """
        {"items":[{"productId":%d,"quantity":%d}],"shippingType":"EXPRESS"}
        """;

    protected static final String PAYMENT_JSON = """
        {"orderId":%d,"amount":%s,"paymentMethod":"CREDIT_CARD"}
        """;
}
