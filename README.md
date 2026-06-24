# Marketplace API

REST API para um marketplace multi-vendedor construída com Spring Boot. Gerencia cadastro de produtos, pedidos, pagamentos, comissões e autenticação JWT com controle de acesso por perfil.

**Banco de dados:** MySQL 8.0 via Docker (produção) | H2 (desenvolvimento/testes)

## Sumário

- [Stack Tecnológica](#stack-tecnológica)
- [Pré-requisitos](#pré-requisitos)
- [Setup](#setup)
- [Execução](#execução)
- [Perfis de Ambiente](#perfis-de-ambiente)
- [Autenticação](#autenticação)
- [API - Endpoints](#api---endpoints)
  - [Auth](#auth-api-auth)
  - [Produtos](#produtos-api-products)
  - [Pedidos](#pedidos-api-orders)
  - [Pagamentos](#pagamentos-api-payments)
- [Formato de Erro](#formato-de-erro)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Modelagem do Banco](#modelagem-do-banco)
- [Padrões de Projeto](#padrões-de-projeto)
- [Testes](#testes)
- [Docker](#docker)
- [Linha do Tempo do Projeto](#linha-do-tempo-do-projeto)

---

## Stack Tecnológica

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.5.15 | Framework principal |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA / Hibernate | — | ORM e persistência |
| H2 Database | 2.x | Banco dev/test |
| MySQL 8.0 | 8.0 | Banco produção (via Docker) |
| JJWT | 0.12.6 | Geração e validação de JWT |
| Lombok | — | Redução de boilerplate |
| SpringDoc OpenAPI | 2.8.16 | Swagger UI |
| Maven Wrapper | — | Build |

---

## Pré-requisitos

### 1. Java 17 JDK

Verifique a instalação:

```bash
java -version
# Saída esperada: openjdk version "17.x" ...
```

Se não tiver, baixe do [Adoptium](https://adoptium.net/temurin/releases/?version=17) ou use SDKMAN:

```bash
sdk install java 17.0.x-tem
```

### 2. Maven (opcional)

O projeto já inclui o Maven Wrapper (`mvnw.cmd`), então o Maven instalado globalmente não é obrigatório. Para verificar se tem:

```bash
mvn -version
```

### 3. Docker (obrigatório para produção)

O projeto usa **MySQL 8.0 via Docker** em produção. Para desenvolvimento o H2 embarcado é suficiente.

```bash
docker --version
```

---

## Setup

### 1. Baixar dependências

```bash
mvnw.cmd dependency:resolve
```

### 2. Compilar

```bash
mvnw.cmd compile
```

### 3. Rodar testes (opcional)

```bash
mvnw.cmd test
```

---

## Execução

### Desenvolvimento (default)

```bash
mvnw.cmd spring-boot:run
```

Acessar: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/api-docs`

### Perfil dev

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Acessar: `http://localhost:8081`

H2 Console: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:file:./data/marketplace_dev`
- User: `sa`
- Password: *(vazio)*

### Produção (MySQL via Docker)

```bash
# 1. Sobe o container MySQL 8.0
docker compose up -d

# 2. Aguarda o MySQL ficar pronto (10-15s)
# Opcional: verificar logs
docker compose logs -f

# 3. Sobe a aplicação com o perfil de produção
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

> **Atenção:** Antes de rodar em produção, altere o secret JWT em `application.properties` ou via variável de ambiente `API_SECURITY_JWT_SECRET`. O docker-compose expõe o MySQL na porta `3306` com usuário `app` e senha `app123`.

---

## Perfis de Ambiente

| Perfil | Banco | DDL | Porta | H2 Console | SQL Log | Uso |
|---|---|---|---|---|---|---|---|
| *(default)* | H2 file (`./data/marketplace_dev`) | `update` | 8080 | Desligado | Sim | Desenvolvimento diário |
| `dev` | H2 file (reseta ao iniciar) | `create-drop` | 8081 | Ligado | Sim | Testes manuais |
| `test` | H2 em memória | `create-drop` | 8080 | — | Não | Testes automatizados |
| `prod` | **MySQL 8.0** (via Docker) | `update` | 8080 | — | Não | Produção |

### Propriedades por perfil

**Default** (`application.properties`):
```properties
spring.datasource.url=jdbc:h2:file:./data/marketplace_dev
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
api.security.jwt.secret=bWFya2V0cGxhY2UtYXBpLWp3dC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=
api.security.jwt.access-expiration=900000    # 15 min
api.security.jwt.refresh-expiration=604800000 # 7 dias
```

**Dev** (`application-dev.properties`):
```properties
server.port=8081
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=create-drop
```

**Prod** (`application-prod.properties`):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/marketplace_api?useSSL=false&serverTimezone=UTC
spring.datasource.username=app
spring.datasource.password=app123
spring.jpa.show-sql=false
```

---

## Autenticação

A API usa **JWT stateless**. O fluxo é:

1. **Registrar** ou **logar** → recebe `token` (access token) e `refreshToken`
2. Enviar o `token` no header `Authorization: Bearer <token>` em requisições autenticadas
3. Quando o token expirar (15 min), usar `/api/auth/refresh` para renovar
4. O refresh token é **rotacionado**: o antigo é revogado e um novo é emitido

### Roles

| Role | Permissões |
|---|---|
| `BUYER` | Criar pedidos, realizar pagamentos |
| `SELLER` | Gerenciar próprios produtos, receber comissões |
| `ADMIN` | Acesso total (ignora verificações de dono) |

---

## API - Endpoints

### Auth (`/api/auth`)

#### `POST /api/auth/register` — Cadastro

**Request:**
```json
{
  "name": "João Vendedor",
  "email": "joao@email.com",
  "password": "123456",
  "phone": "11999999999",
  "role": "SELLER"
}
```

**Response** `201 Created`:
```json
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "a1b2c3d4-..."
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"João","email":"joao@email.com","password":"123456","phone":"11999999999","role":"SELLER"}'
```

---

#### `POST /api/auth/login` — Login

**Request:**
```json
{
  "email": "joao@email.com",
  "password": "123456"
}
```

**Response** `200 OK`:
```json
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "e5f6g7h8-..."
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@email.com","password":"123456"}'
```

---

#### `POST /api/auth/refresh` — Renovar token

**Request:**
```json
{
  "refreshToken": "e5f6g7h8-..."
}
```

**Response** `200 OK`:
```json
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJ...novo...",
  "refreshToken": "i9j0k1l2-..."
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"e5f6g7h8-..."}'
```

---

### Produtos (`/api/products`)

#### `POST /api/products` — Criar (SELLER, ADMIN)

**Request:**
```json
{
  "name": "Notebook Gamer",
  "description": "RTX 4060, 16GB RAM",
  "price": 4999.99,
  "stockQuantity": 10
}
```

**Response** `201 Created`:
```json
{
  "id": 1,
  "name": "Notebook Gamer",
  "description": "RTX 4060, 16GB RAM",
  "price": 4999.99,
  "stockQuantity": 10,
  "sellerId": 1,
  "sellerName": "João Vendedor",
  "createdAt": "2026-06-23T20:00:00",
  "updatedAt": "2026-06-23T20:00:00"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"name":"Notebook Gamer","description":"RTX 4060","price":4999.99,"stockQuantity":10}'
```

---

#### `GET /api/products` — Listar (público)

```bash
curl http://localhost:8080/api/products
```

**Response** `200 OK`:
```json
[
  {
    "id": 1,
    "name": "Notebook Gamer",
    "price": 4999.99,
    "stockQuantity": 10,
    "sellerName": "João Vendedor",
    ...
  }
]
```

**Com filtro por nome:**
```bash
curl "http://localhost:8080/api/products?name=notebook"
```

---

#### `GET /api/products/{id}` — Buscar por ID (público)

```bash
curl http://localhost:8080/api/products/1
```

---

#### `PUT /api/products/{id}` — Atualizar (dono, ADMIN)

**Request:**
```json
{
  "name": "Notebook Gamer Ultra",
  "description": "RTX 4070, 32GB RAM",
  "price": 5999.99,
  "stockQuantity": 5
}
```

**cURL:**
```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"name":"Notebook Gamer Ultra","description":"RTX 4070","price":5999.99,"stockQuantity":5}'
```

---

#### `DELETE /api/products/{id}` — Excluir (dono, ADMIN)

```bash
curl -X DELETE http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Response** `204 No Content`

---

### Pedidos (`/api/orders`)

#### `POST /api/orders` — Criar (BUYER)

**Request:**
```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ],
  "shippingType": "SEDEX"
}
```

**Response** `201 Created`:
```json
{
  "id": 1,
  "buyerId": 2,
  "buyerName": "Maria Compradora",
  "items": [
    { "productId": 1, "productName": "Notebook Gamer", "quantity": 2, "unitPrice": 4999.99, "subtotal": 9999.98 },
    { "productId": 2, "productName": "Mouse RGB", "quantity": 1, "unitPrice": 149.90, "subtotal": 149.90 }
  ],
  "totalAmount": 10174.88,
  "shippingAmount": 25.00,
  "shippingType": "SEDEX",
  "status": "PENDING",
  "createdAt": "2026-06-23T20:00:00"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"items":[{"productId":1,"quantity":2}],"shippingType":"SEDEX"}'
```

**Shipping types disponíveis:** `EXPRESS`, `ECONOMIC`, `SEDEX`, `PAC`

---

#### `GET /api/orders` — Listar meus pedidos (autenticado)

```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

#### `GET /api/orders/{id}` — Buscar pedido (dono, ADMIN)

```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

#### `POST /api/orders/{id}/cancel` — Cancelar (dono, ADMIN)

```bash
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Response** `204 No Content`

> O estoque dos produtos é **restaurado** automaticamente ao cancelar.

---

### Pagamentos (`/api/payments`)

#### `POST /api/payments` — Processar pagamento (BUYER, dono do pedido)

**Request:**
```json
{
  "orderId": 1,
  "amount": 10174.88,
  "paymentMethod": "CREDIT_CARD"
}
```

**Response** `201 Created`:
```json
{
  "id": 1,
  "orderId": 1,
  "amount": 10174.88,
  "status": "APPROVED",
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "txn_abc123",
  "createdAt": "2026-06-23T20:05:00"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"orderId":1,"amount":10174.88,"paymentMethod":"CREDIT_CARD"}'
```

---

#### `GET /api/payments/order/{orderId}` — Buscar pagamento por pedido (dono)

```bash
curl http://localhost:8080/api/payments/order/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Formato de Erro

Todas as exceções retornam um JSON padronizado:

```json
{
  "timestamp": "2026-06-23T20:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Descrição do erro"
}
```

### Códigos HTTP

| Status | Quando ocorre |
|---|---|
| `400 Bad Request` | Validação de campos (ex: email inválido, campo obrigatório) |
| `402 Payment Required` | Erro no processamento do pagamento |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Estoque insuficiente ou violação de integridade |
| `422 Unprocessable Entity` | Regra de negócio (ex: usuário não autenticado, permissão negada) |
| `500 Internal Server Error` | Erro inesperado |

### Erro de validação (`400`)

```json
{
  "timestamp": "2026-06-23T20:00:00",
  "status": 400,
  "errors": {
    "email": "must be a well-formed email address",
    "password": "must not be blank"
  }
}
```

---

## Estrutura do Projeto

```
src/main/java/com/marketplace/api/
├── ApiApplication.java                          # Main class (@EnableRetry, @EnableScheduling)
│
├── common/
│   └── OwnershipValidator.java                  # Valida se o usuário é dono do recurso (ou ADMIN)
│
├── config/
│   ├── JwtAuthenticationFilter.java             # Filtro OncePerRequestFilter que valida o token JWT
│   ├── SecurityConfig.java                      # Configuração de segurança (CSRF, CORS, rotas públicas)
│   ├── SwaggerConfig.java                       # Configuração do OpenAPI / Swagger UI
│   └── WebConfig.java                           # Configuração CORS global
│
├── controller/
│   ├── AuthController.java                      # /api/auth    (register, login, refresh)
│   ├── OrderController.java                     # /api/orders  (CRUD + cancelamento)
│   ├── PaymentController.java                   # /api/payments (processar + consultar)
│   └── ProductController.java                   # /api/products (CRUD + busca por nome)
│
├── dto/
│   ├── request/
│   │   ├── AuthRequest.java                     # Login: email + password
│   │   ├── RegisterRequest.java                 # Cadastro: name, email, password, phone, role
│   │   ├── RefreshTokenRequest.java             # Refresh: refreshToken
│   │   ├── ProductRequest.java                  # Produto: name, description, price, stockQuantity
│   │   ├── OrderRequest.java                    # Pedido: items + shippingType
│   │   ├── OrderItemRequest.java                # Item: productId + quantity
│   │   └── PaymentRequest.java                  # Pagamento: orderId + amount + paymentMethod
│   └── response/
│       ├── AuthResponse.java                    # id, name, email, role, token, refreshToken
│       ├── ProductResponse.java                 # id, name, description, price, stock, seller, timestamps
│       ├── OrderResponse.java                   # id, buyer, items, totals, shipping, status, timestamps
│       ├── OrderItemResponse.java               # productId, productName, quantity, unitPrice, subtotal
│       └── PaymentResponse.java                 # id, orderId, amount, status, method, transactionId
│
├── entity/
│   ├── User.java                                # users: id, name, email, password, phone, role
│   ├── Product.java                             # products: id, name, description, price, stock, seller, version
│   ├── Order.java                               # orders: id, buyer, items, totalAmount, shipping, status
│   ├── OrderItem.java                           # order_items: id, order, product, quantity, unitPrice, subtotal
│   ├── Payment.java                             # payments: id, order, amount, status, method, transactionId
│   ├── RefreshToken.java                        # refresh_tokens: id, token, user, expiresAt, revokedAt
│   ├── Commission.java                          # commissions: id, order, seller, amount, paid
│   └── enums/
│       ├── Role.java                            # BUYER, SELLER, ADMIN
│       ├── OrderStatus.java                     # PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
│       ├── PaymentStatus.java                   # PENDING, APPROVED, DECLINED, REFUNDED, CANCELLED
│       └── ShippingType.java                    # EXPRESS, ECONOMIC, SEDEX, PAC
│
├── exception/
│   ├── BusinessException.java                   # HTTP 422
│   ├── ResourceNotFoundException.java           # HTTP 404
│   ├── InsufficientStockException.java          # HTTP 409
│   ├── PaymentProcessingException.java          # HTTP 402
│   └── GlobalExceptionHandler.java              # @RestControllerAdvice: centraliza tratamento de erros
│
├── mapper/
│   ├── ProductMapper.java                       # Product <-> ProductResponse, ProductRequest -> Product
│   ├── OrderMapper.java                         # Order -> OrderResponse
│   └── PaymentMapper.java                       # Payment -> PaymentResponse
│
├── repository/
│   ├── UserRepository.java                      # findByEmail()
│   ├── ProductRepository.java                   # findBySeller(), findByNameContainingIgnoreCase()
│   ├── OrderRepository.java                     # findByBuyer()
│   ├── OrderItemRepository.java                 # (métodos padrão JPA)
│   ├── PaymentRepository.java                   # findByOrder()
│   ├── RefreshTokenRepository.java              # findByToken(), deleteAllExpiredSince()
│   └── CommissionRepository.java                # findBySeller()
│
└── service/
    ├── AuthService.java                         # Lógica de registro, login e refresh token
    ├── JwtService.java                          # Geração e validação de tokens JWT
    ├── CustomUserDetailsService.java            # Implementação do UserDetailsService do Spring Security
    ├── SecurityService.java                     # Interface: getAuthenticatedUser(), requireRole()
    ├── SecurityServiceImpl.java                 # Implementação: lê SecurityContextHolder
    ├── RefreshTokenService.java                 # Criação, rotação, revogação e limpeza de refresh tokens
    ├── ProductService.java                      # CRUD de produtos com verificação de dono
    ├── OrderService.java                        # Criação, listagem, cancelamento de pedidos
    ├── InventoryService.java                    # Reserva e liberação de estoque com @Retryable
    ├── PaymentService.java                      # Processamento de pagamentos
    ├── CommissionService.java                   # Interface: saveCommissions(), findBySeller()
    ├── CommissionServiceImpl.java               # Distribuição proporcional de comissão entre vendedores
    ├── factory/
    │   ├── ShippingStrategyFactory.java         # Factory que descobre estratégias de frete via @Component
    │   └── CommissionStrategyFactory.java       # Factory que descobre estratégias de comissão via @Component
    └── strategy/
        ├── ShippingStrategy.java                # Interface: calculate(Order)
        ├── ExpressShipping.java                 # 10% do total do pedido
        ├── EconomicShipping.java                # R$ 15,00 fixo
        ├── SedexShipping.java                   # R$ 25,00 fixo
        ├── PacShipping.java                     # R$ 20,00 fixo
        ├── CommissionStrategy.java              # Interface: calculate(Order)
        ├── StandardCommission.java              # 5% do total
        └── PremiumCommission.java               # 3% do total
```

```
src/test/java/com/marketplace/api/
├── ApiApplicationTests.java                     # Teste de contexto
├── controller/
│   └── OrderControllerTest.java                 # Testes do controller de pedidos (4 testes)
├── integration/
│   ├── AbstractIntegrationTest.java             # Base class com MockMvc, helpers, cleanup
│   ├── AuthIntegrationTest.java                 # Testes completos de registro e login (5 testes)
│   ├── ProductIntegrationTest.java              # Testes completos de produtos (8 testes)
│   ├── OrderIntegrationTest.java                # Testes completos de pedidos (8 testes)
│   └── PaymentIntegrationTest.java              # Testes completos de pagamentos (7 testes)
└── service/
    ├── OrderServiceTest.java                    # Mockito: lógica de pedidos (11 testes)
    ├── ProductServiceTest.java                  # Mockito: lógica de produtos (10 testes)
    ├── PaymentServiceTest.java                  # Mockito: lógica de pagamentos (7 testes)
    ├── InventoryServiceTest.java                # Mockito: reserva de estoque (2 testes)
    ├── SecurityServiceTest.java                 # Mockito: autenticação e roles (5 testes)
    ├── CommissionServiceTest.java               # Mockito: distribuição de comissões (3 testes)
    ├── ShippingStrategyTest.java                # Teste das estratégias de frete (4 testes)
    └── OwnershipValidatorTest.java              # Teste do validador de dono (3 testes)
```

---

## Modelagem do Banco

```mermaid
erDiagram
    User ||--o{ Product : "seller"
    User ||--o{ Order : "buyer"
    User ||--o{ RefreshToken : ""
    User ||--o{ Commission : "seller"

    Order ||--o{ OrderItem : "contém"
    Order ||--o| Payment : "tem"
    Order ||--o{ Commission : "gera"

    Product ||--o{ OrderItem : ""
```

### Tabelas

| Tabela | Colunas principais |
|---|---|
| `users` | id (PK), name, email (unique), password, phone, role, created_at, updated_at |
| `products` | id (PK), name, description, price, stock_quantity, seller_id (FK), version, created_at, updated_at |
| `orders` | id (PK), buyer_id (FK), total_amount, shipping_amount, shipping_type, status, created_at, updated_at |
| `order_items` | id (PK), order_id (FK), product_id (FK), quantity, unit_price, subtotal |
| `payments` | id (PK), order_id (FK unique), amount, status, payment_method, transaction_id, created_at |
| `refresh_tokens` | id (PK), token (unique), user_id (FK), expires_at, revoked_at, created_at |
| `commissions` | id (PK), order_id (FK), seller_id (FK), amount, paid, created_at |

---

## Padrões de Projeto

### Strategy + Factory — Frete e Comissão

As estratégias de frete (`ShippingStrategy`) e comissão (`CommissionStrategy`) são interfaces que qualquer `@Component` pode implementar. As factories (`ShippingStrategyFactory`, `CommissionStrategyFactory`) injetam uma `List<Strategy>` do Spring e montam um `Map<String, Strategy>` por tipo. Assim, para adicionar uma nova estratégia basta criar uma classe com `@Component` — nenhum código existente é alterado (OCP).

### DTO (Data Transfer Object)

Request e response têm classes próprias, desacopladas das entidades JPA. Mappers (`ProductMapper`, `OrderMapper`, `PaymentMapper`) convertem entre os layers.

### Ownership Validation

O `OwnershipValidator.validateOwnership()` compara o ID do dono do recurso com o ID do usuário autenticado. ADMIN tem bypass automático.

### Optimistic Locking com Retry

A entidade `Product` usa `@Version` para lock otimista. `InventoryService` anota `reserveStock()` e `releaseStock()` com `@Retryable` (até 3 tentativas com backoff exponencial) para tratar `ObjectOptimisticLockingFailureException` em cenários de concorrência.

### Global Exception Handler

`@RestControllerAdvice` centraliza toda exception em um JSON padronizado com timestamp, status HTTP e mensagem.

### Scheduled Task

`RefreshTokenService.cleanupExpired()` roda a cada 24h (`@Scheduled`) para deletar tokens expirados/revogados.

---

## Testes

### Executar

```bash
# Todos os testes (≈78 casos em 15 classes)
mvnw.cmd test

# Classe específica
mvnw.cmd test -Dtest=OrderServiceTest

# Método específico
mvnw.cmd test -Dtest=OrderServiceTest#shouldCreateOrderSuccessfully

# Pacote específico
mvnw.cmd test "-Dtest=com.marketplace.api.service.*"
```

### Categorias

| Tipo | Tecnologia | O que testa | Quantidade |
|---|---|---|---|
| Unitário | Mockito | Lógica de serviço com dependências mockadas | ~45 testes |
| Controller | `@WebMvcTest` + Mockito | Camada HTTP com serviços mockados | 4 testes |
| Integração | `@SpringBootTest` + MockMvc + H2 | Fluxo completo com banco embeddado | ~28 testes |

### Estrutura dos testes de integração

A classe base `AbstractIntegrationTest` fornece:

- `@SpringBootTest` com `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")` — usa H2 em memória isolado
- `@BeforeEach` que limpa todas as tabelas
- Helpers: `createUser(email, role)`, `createProduct(seller)`, `tokenFor(user)`, `json(content)`, `extractId(response)`

---

## Docker

O projeto usa **MySQL 8.0** em produção via Docker. O container é definido no `docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: marketplace-api-mysql
    restart: unless-stopped
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: marketplace-api
      MYSQL_USER: app
      MYSQL_PASSWORD: app123
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

### Comandos

```bash
# Iniciar MySQL
docker compose up -d

# Verificar logs
docker compose logs -f

# Parar (mantém dados)
docker compose stop

# Parar e remover container
docker compose down

# Parar, remover container e apagar volume (dados)
docker compose down -v

# Acessar o MySQL via terminal
docker exec -it marketplace-api-mysql mysql -u app -p
```

### Credenciais do MySQL

| Propriedade | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3306` |
| Database | `marketplace-api` |
| Usuário | `app` |
| Senha | `app123` |
| Root password | `root123` |

---

## Linha do Tempo do Projeto

O histórico de commits segue o [Conventional Commits](https://www.conventionalcommits.org/) e reflete a construção do projeto por camadas:

| Commit | Descrição |
|---|---|
| `chore: initial project setup with Spring Boot 3.5.15 and Maven Wrapper` | Estrutura inicial do projeto, `pom.xml`, `mvnw`, `.gitignore`, propriedades padrão |
| `feat: add JPA entities and enums for domain model` | Entidades `User`, `Product`, `Order`, `OrderItem`, `Payment`, `RefreshToken`, `Commission` + enums `Role`, `OrderStatus`, `PaymentStatus`, `ShippingType` |
| `feat: add Spring Data JPA repositories` | Interfaces `UserRepository`, `ProductRepository`, `OrderRepository`, `OrderItemRepository`, `PaymentRepository`, `RefreshTokenRepository`, `CommissionRepository` |
| `feat: add request/response DTOs and entity mappers` | DTOs de request (`AuthRequest`, `RegisterRequest`, `ProductRequest`, `OrderRequest`, etc.) e response (`AuthResponse`, `ProductResponse`, `OrderResponse`, `PaymentResponse`) + mappers |
| `feat: add custom exceptions and global exception handler` | `BusinessException`, `ResourceNotFoundException`, `InsufficientStockException`, `PaymentProcessingException`, `GlobalExceptionHandler` com `@RestControllerAdvice` |
| `feat: add JWT authentication and security configuration` | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `CustomUserDetailsService`, `SecurityService`/`SecurityServiceImpl` |
| `feat: add business services layer` | `AuthService`, `RefreshTokenService`, `ProductService`, `OrderService`, `InventoryService`, `PaymentService` |
| `feat: add commission and shipping strategies with factories` | `CommissionService`/`CommissionServiceImpl`, `ShippingStrategy`, `CommissionStrategy` com implementações e factories |
| `feat: add REST controllers with role-based access` | `AuthController`, `ProductController`, `OrderController`, `PaymentController` |
| `feat: add Swagger/OpenAPI documentation` | `SwaggerConfig` com SpringDoc OpenAPI 2.8.16 |
| `feat: add CORS configuration` | `WebConfig` permitindo todas origens em desenvolvimento |
| `test: add unit tests for service layer` | Testes Mockito para `OrderService`, `ProductService`, `PaymentService`, `InventoryService`, `SecurityService`, `CommissionService`, `ShippingStrategy`, `OwnershipValidator` |
| `test: add controller tests` | Testes `@WebMvcTest` para `OrderController` |
| `test: add integration tests` | Testes `@SpringBootTest` + MockMvc para autenticação, produtos, pedidos e pagamentos |
| `docs: add README with API documentation` | Documentação completa com endpoints, exemplos, setup e arquitetura |
| `chore: add Docker Compose for MySQL 8.0` | `docker-compose.yml` com MySQL 8.0 para ambiente de produção |
