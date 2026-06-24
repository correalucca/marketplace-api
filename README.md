<p align="center">
  <h1 align="center">Marketplace API</h1>
  <p align="center">
    REST API para marketplace multi-vendedor com Spring Boot
    <br />
    <a href="#instalação"><strong>Instalação »</strong></a>
    <br />
    <br />
    <a href="#endpoints">Endpoints</a>
    ·
    <a href="#testes">Testes</a>
    ·
    <a href="#contribuição">Contribuição</a>
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.15-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL 8.0"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/license-MIT-green?style=for-the-badge" alt="MIT License"/>
</p>

---

## Sobre o Projeto

API backend para um marketplace onde **vendedores** cadastram produtos, **compradores** criam pedidos e realizam pagamentos, e as **comissões** são distribuídas proporcionalmente entre os vendedores de cada pedido.

**Funcionalidades principais:**

- Autenticação JWT com refresh token rotation
- CRUD de produtos com busca por nome
- Criação e cancelamento de pedidos com reserva de estoque
- Processamento de pagamentos
- Cálculo de frete por estratégia (Express, Econômico, Sedex, PAC)
- Distribuição proporcional de comissões entre vendedores
- Três perfis de acesso: BUYER, SELLER, ADMIN
- Lock otimista com retry automático para concorrência de estoque

**Stack:** Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, MySQL 8.0 (Docker), H2 (dev/test), JWT, Swagger.

---

## Stack Tecnológica

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.5.15 | Framework |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA / Hibernate | — | ORM |
| MySQL 8.0 | 8.0 | Banco produção (via Docker) |
| H2 Database | 2.x | Banco dev/test |
| JJWT | 0.12.6 | Tokens JWT |
| Lombok | — | Boilerplate |
| SpringDoc OpenAPI | 2.8.16 | Swagger UI |
| Maven Wrapper | — | Build |

---

## Pré-requisitos

Antes de começar, você precisa ter instalado:

- **Java 17+** — [Download](https://adoptium.net/temurin/releases/?version=17)
  ```bash
  java -version
  # openjdk version "17.x" ...
  ```
- **Docker** — Para subir o MySQL em produção
  ```bash
  docker --version
  ```
- **Maven** (opcional) — O projeto já inclui o `mvnw.cmd` (Maven Wrapper)

---

## Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/marketplace-api.git
cd marketplace-api
```

### 2. Baixe as dependências

```bash
mvnw.cmd dependency:resolve
```

### 3. Compile o projeto

```bash
mvnw.cmd compile
```

### 4. Execute os testes (opcional)

```bash
mvnw.cmd test
```

---

## Uso

### Desenvolvimento (H2 embarcado)

O perfil padrão usa H2 em arquivo — não precisa de Docker. Os dados persistem entre execuções.

```bash
mvnw.cmd spring-boot:run
```

Acesse: [`http://localhost:8080`](http://localhost:8080)

Swagger UI: [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

### Perfil dev (H2 com console)

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Acesse: [`http://localhost:8081`](http://localhost:8081)

H2 Console: [`http://localhost:8081/h2-console`](http://localhost:8081/h2-console) (JDBC: `jdbc:h2:file:./data/marketplace_dev`, usuário: `sa`, senha: vazia)

### Produção (MySQL via Docker)

```bash
# 1. Inicie o MySQL
docker compose up -d

# 2. Aguarde o MySQL ficar pronto (~15s)
# 3. Inicie a aplicação com o perfil de produção
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

> ⚠️ **Importante:** Altere o `api.security.jwt.secret` antes de usar em produção. Você pode sobrescrever via variável de ambiente `API_SECURITY_JWT_SECRET`.

### Perfis

| Perfil | Banco | DDL | Porta | H2 Console | SQL Log |
|---|---|---|---|---|---|
| *(default)* | H2 file | `update` | 8080 | ❌ | ✅ |
| `dev` | H2 file | `create-drop` | 8081 | ✅ | ✅ |
| `test` | H2 memória | `create-drop` | 8080 | — | ❌ |
| `prod` | **MySQL 8.0** (Docker) | `update` | 8080 | — | ❌ |

---

## Autenticação

A API usa **JWT stateless** com refresh token rotation.

1. **Registre** ou **faça login** — recebe `token` (válido por 15 min) e `refreshToken` (válido por 7 dias)
2. Envie o `token` no header: `Authorization: Bearer <token>`
3. Quando expirar, chame `/api/auth/refresh` com o `refreshToken` para obter um novo par
4. O refresh token antigo é **revogado** e um novo é emitido

### Roles

| Role | Acesso |
|---|---|
| `BUYER` | Criar pedidos, realizar pagamentos |
| `SELLER` | Gerenciar próprios produtos, receber comissões |
| `ADMIN` | Acesso total (ignora verificação de dono) |

---

## Endpoints

### Auth — `/api/auth`

#### `POST /api/auth/register`
Registra um novo usuário.

```json
// Request
{
  "name": "João Vendedor",
  "email": "joao@email.com",
  "password": "123456",
  "phone": "11999999999",
  "role": "SELLER"
}

// Response 201
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"João","email":"joao@email.com","password":"123456","phone":"11999999999","role":"SELLER"}'
```

#### `POST /api/auth/login`
Autentica com email e senha.

```json
// Request
{
  "email": "joao@email.com",
  "password": "123456"
}

// Response 200
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@email.com","password":"123456"}'
```

#### `POST /api/auth/refresh`
Renova o access token usando o refresh token.

```json
// Request
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

// Response 200
{
  "id": 1,
  "name": "João Vendedor",
  "email": "joao@email.com",
  "role": "SELLER",
  "token": "eyJ...novo...",
  "refreshToken": "660e8400-e29b-41d4-a716-446655440001"
}
```

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

---

### Produtos — `/api/products`

#### `POST /api/products`
Cria um produto. Requer role `SELLER` ou `ADMIN`.

```json
// Request
{
  "name": "Notebook Gamer",
  "description": "RTX 4060, 16GB RAM",
  "price": 4999.99,
  "stockQuantity": 10
}

// Response 201
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

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"name":"Notebook Gamer","description":"RTX 4060","price":4999.99,"stockQuantity":10}'
```

#### `GET /api/products` — Listar todos (público)

```bash
# Listar todos
curl http://localhost:8080/api/products

# Buscar por nome
curl "http://localhost:8080/api/products?name=notebook"
```

#### `GET /api/products/{id}` — Buscar por ID (público)

```bash
curl http://localhost:8080/api/products/1
```

#### `PUT /api/products/{id}`
Atualiza um produto. Requer ser o **dono** ou `ADMIN`.

```json
// Request
{
  "name": "Notebook Gamer Ultra",
  "price": 5999.99,
  "stockQuantity": 5
}
```

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"name":"Notebook Gamer Ultra","price":5999.99,"stockQuantity":5}'
```

#### `DELETE /api/products/{id}`
Exclui um produto. Requer ser o **dono** ou `ADMIN`.

```bash
curl -X DELETE http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Response 204 No Content
```

---

### Pedidos — `/api/orders`

#### `POST /api/orders`
Cria um pedido. Requer role `BUYER`.

```json
// Request
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ],
  "shippingType": "SEDEX"
}

// Response 201
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
  "createdAt": "2026-06-23T20:00:00",
  "updatedAt": "2026-06-23T20:00:00"
}
```

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"items":[{"productId":1,"quantity":2}],"shippingType":"SEDEX"}'
```

**Tipos de frete:** `EXPRESS`, `ECONOMIC`, `SEDEX`, `PAC`

#### `GET /api/orders` — Listar meus pedidos (autenticado)

```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### `GET /api/orders/{id}` — Buscar pedido (dono ou ADMIN)

```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

#### `POST /api/orders/{id}/cancel` — Cancelar pedido (dono ou ADMIN)

```bash
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Response 204 No Content
```

> O estoque dos produtos é **restaurado automaticamente** ao cancelar o pedido.

---

### Pagamentos — `/api/payments`

#### `POST /api/payments`
Processa o pagamento de um pedido. Requer ser o **dono** do pedido.

```json
// Request
{
  "orderId": 1,
  "amount": 10174.88,
  "paymentMethod": "CREDIT_CARD"
}

// Response 201
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

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"orderId":1,"amount":10174.88,"paymentMethod":"CREDIT_CARD"}'
```

#### `GET /api/payments/order/{orderId}` — Consultar pagamento (dono)

```bash
curl http://localhost:8080/api/payments/order/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### Tratamento de Erros

Todas as exceções retornam um JSON padronizado:

```json
{
  "timestamp": "2026-06-23T20:00:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Descrição do erro"
}
```

**Códigos HTTP:**

| Status | Quando ocorre |
|---|---|
| `400` | Erro de validação de campos |
| `402` | Erro no processamento do pagamento |
| `404` | Recurso não encontrado |
| `409` | Estoque insuficiente ou violação de integridade |
| `422` | Regra de negócio (permissão, autenticação) |
| `500` | Erro interno do servidor |

Erro de validação (`400`):

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
├── ApiApplication.java              # Main class (@EnableRetry, @EnableScheduling)
├── common/                          # OwnershipValidator
├── config/                          # Security, JWT filter, CORS, Swagger
├── controller/                      # REST controllers (4)
├── dto/                             # Request/Response DTOs
├── entity/                          # JPA entities (7) + enums (4)
├── exception/                       # Custom exceptions (4) + GlobalExceptionHandler
├── mapper/                          # Entity <-> DTO mappers
├── repository/                      # Spring Data JPA repositories (7)
└── service/                         # Business services + factories + strategies
    ├── factory/                     # ShippingStrategyFactory, CommissionStrategyFactory
    └── strategy/                    # ShippingStrategy (4), CommissionStrategy (2)
```

---

## Docker

O banco de produção é **MySQL 8.0** rodando via Docker Compose:

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: marketplace-api-mysql
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: marketplace-api
      MYSQL_USER: app
      MYSQL_PASSWORD: app123
    volumes: [mysql_data:/var/lib/mysql]

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

# Parar, remover container + volume (apaga dados)
docker compose down -v

# Acessar terminal MySQL
docker exec -it marketplace-api-mysql mysql -u app -p
```

### Credenciais

| Propriedade | Valor |
|---|---|
| Host | `localhost` |
| Porta | `3306` |
| Database | `marketplace-api` |
| Usuário | `app` |
| Senha | `app123` |

---

## Modelagem do Banco

```
User (1) ----< Product (seller)
User (1) ----< Order (buyer)
User (1) ----< RefreshToken
User (1) ----< Commission (seller)
Order (1) ----< OrderItem
Order (1) ----> Payment
Order (1) ----< Commission
Product (1) ----< OrderItem
```

### Tabelas

| Tabela | Colunas principais |
|---|---|
| `users` | id (PK), name, email (unique), password, phone, role |
| `products` | id (PK), name, description, price, stock_quantity, seller_id (FK), version |
| `orders` | id (PK), buyer_id (FK), total_amount, shipping_amount, shipping_type, status |
| `order_items` | id (PK), order_id (FK), product_id (FK), quantity, unit_price, subtotal |
| `payments` | id (PK), order_id (FK unique), amount, status, payment_method, transaction_id |
| `refresh_tokens` | id (PK), token (unique), user_id (FK), expires_at, revoked_at |
| `commissions` | id (PK), order_id (FK), seller_id (FK), amount, paid |

---

## Padrões de Projeto

### Strategy + Factory
`ShippingStrategy` e `CommissionStrategy` são interfaces implementadas por componentes `@Component`. As factories injetam uma `List<Strategy>` e montam um mapa por tipo. Para adicionar nova estratégia, basta criar uma nova classe — nenhum código existente é alterado.

### DTO
Request e Response são classes separadas das entidades JPA. Mappers fazem a conversão.

### Ownership Validation
`OwnershipValidator` compara o ID do recurso com o ID do usuário autenticado. ADMIN tem bypass automático.

### Optimistic Locking + Retry
`Product` usa `@Version`. `InventoryService` anota `reserveStock()` e `releaseStock()` com `@Retryable` (até 3 tentativas, backoff exponencial) para concorrência.

### Global Exception Handler
`@RestControllerAdvice` centraliza erros em JSON padronizado.

### Scheduled Task
`RefreshTokenService.cleanupExpired()` roda a cada 24h via `@Scheduled`.

---

## Testes

O projeto possui **~78 testes** distribuídos em **15 classes**:

| Tipo | Tecnologia | Quantidade |
|---|---|---|
| Unitário (Mockito) | Serviços mockados | ~45 |
| Controller (`@WebMvcTest`) | Camada HTTP | 4 |
| Integração (`@SpringBootTest`) | Fluxo completo com H2 | ~28 |

```bash
# Executar todos os testes
mvnw.cmd test

# Classe específica
mvnw.cmd test -Dtest=OrderServiceTest

# Método específico
mvnw.cmd test -Dtest=OrderServiceTest#shouldCreateOrderSuccessfully

# Pacote
mvnw.cmd test "-Dtest=com.marketplace.api.service.*"
```

---

## Contribuição

Contribuições são bem-vindas! Siga os passos:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feat/nova-feature`)
3. Commit suas alterações seguindo [Conventional Commits](https://www.conventionalcommits.org/)
4. Faça push (`git push origin feat/nova-feature`)
5. Abra um Pull Request

### Padrão de commits

```
feat: add new feature
fix: correct bug
test: add tests
docs: update documentation
refactor: code change without fix or feature
chore: build, config, dependencies
```

---

## Licença

Distribuído sob licença MIT. Veja [LICENSE](LICENSE) para mais informações.
docs: add README with API documentation and Docker Compose for MySQL 8.0
```
