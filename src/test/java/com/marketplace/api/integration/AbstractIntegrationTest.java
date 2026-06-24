package com.marketplace.api.integration;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.api.service.JwtService;
import com.marketplace.api.entity.Product;
import com.marketplace.api.entity.User;
import com.marketplace.api.entity.enums.Role;
import com.marketplace.api.repository.CommissionRepository;
import com.marketplace.api.repository.OrderRepository;
import com.marketplace.api.repository.PaymentRepository;
import com.marketplace.api.repository.ProductRepository;
import com.marketplace.api.repository.RefreshTokenRepository;
import com.marketplace.api.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
/**
 * Classe base para os testes de integração.
 * <p>
 * Fornece para as subclasses:
 * <ul>
 *   <li>Contexto Spring real (@SpringBootTest + MockMvc com security)</li>
 *   <li>Banco H2 em memória (isolado do banco de desenvolvimento)</li>
 *   <li>Limpeza automática do banco antes de cada teste</li>
 *   <li>Helpers: createUser(), createProduct(), tokenFor(), json(), extractId()</li>
 * </ul>
 * As subclasses herdam isso e só precisam escrever os cenários de teste.
 */
public abstract class AbstractIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
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
        paymentRepository.deleteAll();
        commissionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
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

    protected Long extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    protected String json(String template, Object... args) {
        return template.formatted(args);
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
