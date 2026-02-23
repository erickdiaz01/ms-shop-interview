# ms-shop — Prueba Técnica Backend

Sistema de microservicios para gestión de catálogo de productos e inventario, construido con Java 21, Spring Boot 3.5, arquitectura hexagonal y comunicación event-driven via Apache Kafka.

---

## Tabla de Contenidos

1. [Instalación y Ejecución](#1-instalación-y-ejecución)
2. [Descripción de la Arquitectura](#2-descripción-de-la-arquitectura)
3. [Decisiones Técnicas y Justificaciones](#3-decisiones-técnicas-y-justificaciones)
4. [Diagrama de Interacción entre Servicios](#4-diagrama-de-interacción-entre-servicios)
5. [Flujo de Compra](#5-flujo-de-compra)
6. [Uso de Herramientas de IA en el Desarrollo](#6-uso-de-herramientas-de-ia-en-el-desarrollo)

---

## 1. Instalación y Ejecución

### Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Docker Desktop | 24.x |
| Docker Compose | v2.24 |
| Java (solo para desarrollo local) | 21 |
| Gradle (solo para desarrollo local) | 8.x |

### Ejecución rápida con Docker Compose

```bash
# 1. Clonar el repositorio
git clone https://github.com/erickdiaz01/ms-shop-interview.git
cd ms-shop-interview

# 2. Levantar el stack completo
docker compose up --build -d

# 3. Verificar que todos los servicios están corriendo (~3-5 minutos la primera vez)
docker compose ps
```

El stack levanta en este orden automáticamente (controlado por `depends_on + healthcheck`):

```
zookeeper → kafka → postgres-productos → postgres-inventario → keycloak
                                                                    ↓
                                              ms-productos → ms-inventario
```

### Servicios disponibles

| Servicio | URL | Credenciales |
|---|---|---|
| ms-productos | http://localhost:8080 | Ver autenticación abajo |
| ms-inventario | http://localhost:8081 | Ver autenticación abajo |
| Swagger ms-productos | http://localhost:8080/swagger-ui.html | — |
| Swagger ms-inventario | http://localhost:8081/swagger-ui.html | — |
| Keycloak Admin | http://localhost:8180 | admin / admin |
| Kafka UI | http://localhost:8090 | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger | http://localhost:16686 | — |

### Autenticación

El sistema soporta dos métodos de autenticación:

**Opción A — JWT via Keycloak** (para clientes externos / Postman):

```bash
# Obtener token
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/microservicios/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=ms-client&client_secret=ms-client-secret' \
  -d 'username=user@test.com&password=test123' \
  | jq -r '.access_token')

# Usar el token
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

Usuarios de prueba disponibles:

| Usuario | Contraseña | Roles |
|---|---|---|
| user@test.com | test123 | ROLE_USER |
| admin@test.com | admin123 | ROLE_ADMIN, ROLE_USER |
| service@test.com | service123 | ROLE_SERVICE |

**Opción B — API Key** (para service-to-service y desarrollo rápido):

```bash
# ms-productos
curl http://localhost:8080/api/v1/products \
  -H 'X-Service-Api-Key: dev-secret-productos-key'

# ms-inventario
curl http://localhost:8081/api/v1/inventory/{productId} \
  -H 'X-Service-Api-Key: dev-secret-inventario-key'
```

### Prueba del flujo completo via cURL

```bash
API_KEY_P="dev-secret-productos-key"
API_KEY_I="dev-secret-inventario-key"

# 1. Crear producto
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/v1/products \
  -H 'Content-Type: application/json' \
  -H "X-Service-Api-Key: $API_KEY_P" \
  -d '{"name":"Laptop Pro 15","price":1299.99,"description":"High-end laptop"}' \
  | jq -r '.data.id')

echo "Producto creado: $PRODUCT_ID"

# 2. Esperar que Kafka inicialice el inventario (~6 segundos)
sleep 6

# 3. Verificar stock inicial = 0
curl -s http://localhost:8081/api/v1/inventory/$PRODUCT_ID \
  -H "X-Service-Api-Key: $API_KEY_I" | jq '.data.attributes.quantity'
# → 0

# 4. Cargar stock
curl -s -X PATCH http://localhost:8081/api/v1/inventory/$PRODUCT_ID \
  -H 'Content-Type: application/json' \
  -H "X-Service-Api-Key: $API_KEY_I" \
  -d '{"quantity":50}' | jq '.data.attributes.quantity'
# → 50

# 5. Realizar compra
curl -s -X POST http://localhost:8081/api/v1/inventory/purchase \
  -H 'Content-Type: application/json' \
  -H "X-Service-Api-Key: $API_KEY_I" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":3}" \
  | jq '.data.attributes'
# → { totalAmount: 3899.97, remainingStock: 47 }
```

### Importar colección Postman

En la carpeta `postman/` se incluyen:
- `microservicios-collection.json` — 20+ requests organizados por carpetas
- `local-docker-environment.json` — variables preconfiguradas

Importar ambos en Postman y seleccionar el environment **Local Docker**.

### Ejecutar tests

```bash
# ms-productos
cd ms-productos && ./gradlew test jacocoTestReport

# ms-inventario
cd ms-inventario && ./gradlew test jacocoTestReport

# Reporte de cobertura
open ms-productos/build/reports/jacoco/test/html/index.html
```

---

## 2. Descripción de la Arquitectura

### Visión general

El sistema implementa dos microservicios de dominio independientes que se comunican de forma asíncrona mediante Apache Kafka, siguiendo el patrón **Outbox** para garantizar consistencia eventual sin pérdida de eventos.

```
ms-productos  →  [Kafka: product.events]  →  ms-inventario
    ↑                                              ↑
    └──────── HTTP (WebClient + Circuit Breaker) ──┘
```

### Arquitectura Hexagonal (Ports & Adapters)

Cada microservicio implementa arquitectura hexagonal con tres capas claramente delimitadas:

```
ms-productos/
└── src/main/java/com/company/productos/
    ├── domain/                          # Núcleo — sin dependencias de frameworks
    │   ├── model/         Product, ProductId, Money
    │   ├── event/         ProductCreatedEvent (DomainEvent)
    │   ├── port/          ProductRepository, OutboxRepository (interfaces)
    │   ├── service/       ProductDomainService
    │   └── exception/     DuplicateProductException, ProductNotFoundException
    │
    ├── application/                     # Casos de uso — orquesta el dominio
    │   └── handler/       CreateProductCommandHandler, GetProductQueryHandler
    │
    └── infrastructure/                  # Adaptadores — implementaciones concretas
        ├── adapter/
        │   ├── in/rest/   ProductController (HTTP)
        │   └── out/
        │       ├── persistence/  ProductRepositoryAdapter (R2DBC)
        │       └── kafka/        ProductEventPublisher (Outbox scheduler)
        └── config/        SecurityConfig, KafkaConfig
```

```
ms-inventario/
└── src/main/java/com/company/inventario/
    ├── domain/
    │   ├── model/         Inventory, Purchase, Quantity
    │   ├── port/          InventoryRepository, PurchaseRepository, ProductPort (ACL)
    │   └── service/       InventoryDomainService
    │
    ├── application/
    │   └── handler/       PurchaseCommandHandler, UpdateStockCommandHandler
    │                      InitializeInventoryUseCase
    │
    └── infrastructure/
        ├── adapter/
        │   ├── in/rest/   InventoryController, PurchaseHistoryController
        │   └── out/
        │       ├── persistence/  InventoryRepositoryAdapter, PurchaseRepositoryAdapter
        │       ├── http/         ProductWebClientAdapter (Circuit Breaker)
        │       └── kafka/        ProductCreatedEventConsumer, InventoryEventProducer
        └── config/        SecurityConfig, KafkaConsumerConfig, ResilienceConfig
```

### Stack tecnológico

| Capa | Tecnología | Justificación |
|---|---|---|
| Runtime | Java 21 | Virtual threads, records, pattern matching |
| Framework | Spring Boot 3.5 + WebFlux | Modelo reactivo no-bloqueante |
| Persistencia | R2DBC + PostgreSQL 16 | Reactive all the way — sin bloqueos |
| Migraciones | Flyway | Versionado de esquema reproducible |
| Mensajería | Apache Kafka 7.6 | At-least-once delivery, log append-only |
| Auth | Keycloak 24 + JWT OAuth2 | OIDC estándar, roles en realm_access |
| Resiliencia | Resilience4j | Circuit Breaker, Retry, Rate Limiter |
| Observabilidad | Prometheus + Grafana + Jaeger | Métricas, dashboards, trazas distribuidas |
| Documentación | SpringDoc OpenAPI 3 | Swagger UI auto-generado |
| Testing | JUnit 5, StepVerifier, Testcontainers | Tests reactivos e integración real |

---

## 3. Decisiones Técnicas y Justificaciones

### 3.1 Dónde implementar el endpoint de compra

**Decisión: el endpoint `/api/v1/inventory/purchase` vive en `ms-inventario`.**

Esta fue la decisión de diseño más importante del sistema. Las alternativas consideradas fueron:

**Alternativa A — Endpoint en ms-productos** (descartada):
- ms-productos conoce el precio del producto, por lo que podría calcular el total.
- Pero ms-productos no conoce el stock ni tiene acceso a la tabla de inventario.
- Obligaría a ms-productos a llamar a ms-inventario para descontar stock → acoplamiento invertido y responsabilidades cruzadas.
- Violaría el principio de que cada microservicio es dueño exclusivo de sus datos.

**Alternativa B — Endpoint en ms-inventario** (elegida):
- ms-inventario es el dueño del agregado `Inventory` y de la tabla `purchase_history`.
- La operación de compra es fundamentalmente una mutación de inventario: validar stock → descontar → registrar historial.
- ms-inventario llama a ms-productos (via `ProductWebClientAdapter`) solo para obtener el precio al momento de la compra, que es un dato de solo lectura.
- Esta llamada está protegida por Circuit Breaker: si ms-productos no está disponible, la compra falla rápido en lugar de colgar el sistema.
- El historial de compras queda en la misma base de datos que el inventario, permitiendo transacciones atómicas.

```
POST /api/v1/inventory/purchase (ms-inventario :8081)
    │
    ├── 1. findByProductId → Inventory (bd inventario)
    ├── 2. GET /products/{id} → precio (ms-productos, vía CB)
    ├── 3. inventory.purchase(qty) → valida stock, lanza excepción si insuficiente
    ├── 4. TX: UPDATE inventory + INSERT purchase_history + INSERT outbox_events
    └── 5. return PurchaseResult { totalAmount, remainingStock }
```

**Conclusión**: el endpoint de compra en ms-inventario mantiene la cohesión del dominio, respeta el principio de Database-per-Service y hace que cada servicio sea responsable único de sus agregados.

### 3.2 Outbox Pattern sobre HTTP directo

**Problema**: si ms-productos publica el evento a Kafka y luego guarda en la BD (o al revés), un fallo entre los dos pasos genera inconsistencia: producto guardado pero sin evento, o evento publicado pero producto no guardado.

**Solución — Transactional Outbox**:
```sql
-- Una sola transacción PostgreSQL:
INSERT INTO products (id, name, price, ...)
INSERT INTO outbox_events (event_type='ProductCreatedEvent', published=false, payload=JSON)
COMMIT
-- Si falla aquí, NADA se persiste. Atomicidad garantizada.

-- Scheduler independiente cada 5s:
SELECT * FROM outbox_events WHERE published = false
kafka.send('product.events', event)
UPDATE outbox_events SET published = true
```

Garantías: cero pérdida de eventos, tolerante a fallos de Kafka, idempotente en el consumer.

### 3.3 Reactive Stack (WebFlux + R2DBC)

Cada request no bloquea un thread del pool — usa el event loop de Netty. Bajo carga alta, un servicio reactivo con 50 threads maneja miles de conexiones concurrentes donde un servicio bloqueante necesitaría miles de threads (y su overhead de memoria).

El costo es mayor complejidad en el código (Mono/Flux en lugar de valores directos) y en los tests (StepVerifier en lugar de asserts directos). Se asume este costo porque los microservicios de inventario hacen múltiples llamadas I/O por request (BD + Kafka + HTTP externo).

### 3.4 Resolución del split-network Docker/JWT (el bug más sutil)

**Problema**: cuando se corre el stack en Docker Compose, Keycloak tiene dos URLs según quién lo llame:
- Postman (fuera de Docker): `http://localhost:8180` → el token JWT tiene `iss = http://localhost:8180/...`
- Microservicio (dentro de Docker): solo alcanza `http://keycloak:8080`

Si se configura `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/...`, Spring hace OIDC discovery desde esa URL y obtiene `issuer=http://keycloak:8080/...`. Luego compara con el claim `iss` del token (`localhost:8180`) → mismatch → **401 en todos los requests aunque el token sea válido**.

**Solución**: `ReactiveJwtDecoder` manual con dos URLs separadas + `KC_HOSTNAME_URL` en Keycloak:

```yaml
# docker-compose.yml — Keycloak
KC_HOSTNAME_URL: http://localhost:8180   # todos los tokens tendrán iss=localhost:8180

# docker-compose.yml — microservicios
JWK_SET_URI:    http://keycloak:8080/.../certs  # descarga claves (URL interna Docker)
JWT_ISSUER_URI: http://localhost:8180/...        # valida claim 'iss' (URL pública)
```

```java
// SecurityConfig.java
private ReactiveJwtDecoder buildJwtDecoder() {
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
            .withJwkSetUri(jwkSetUri)   // URL interna: funciona dentro de Docker
            .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(jwtIssuerUri)  // URL pública: coincide con el token
    ));
    return decoder;
}
```

### 3.5 Circuit Breaker en la comunicación entre servicios

`ProductWebClientAdapter` en ms-inventario llama a ms-productos via HTTP. Sin protección, si ms-productos está caído o lento, cada request de compra espera el timeout (por defecto varios segundos) antes de fallar. Con 100 usuarios simultáneos esto agota el pool de conexiones de ms-inventario.

Configuración Resilience4j:
- **Umbral**: 50% de fallos en ventana de 10 requests → abre el circuito
- **Tiempo abierto**: 30 segundos → rechaza inmediatamente sin llamar a ms-productos
- **Half-open**: 5 requests de prueba → si pasan, cierra el circuito
- **Retry**: 3 intentos con backoff exponencial antes de contar como fallo

### 3.6 Seguridad en tests: omitir oauth2ResourceServer cuando no hay Keycloak

Cuando `app.security.jwk-set-uri` está vacío (perfil test/local), el `SecurityConfig` omite completamente la configuración de `oauth2ResourceServer`. Si se llama `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` sin un decoder explícito, Spring busca automáticamente un bean `ReactiveJwtDecoder` o la propiedad `issuer-uri`. Al no encontrarlos en tests → `NoSuchBeanDefinitionException` → el contexto no levanta.

```java

// Solo configurar JWT si hay Keycloak disponible
if (jwkSetUri != null && !jwkSetUri.isBlank()) {
    chain.oauth2ResourceServer(oauth2 -> oauth2.jwt(...));
}

```

---

## 4. Diagrama de Interacción entre Servicios

### Vista completa del sistema

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENTES EXTERNOS                            │
│              Postman / Apps (JWT Bearer o API Key)                  │
└──────────────────────┬──────────────────────┬───────────────────────┘
                       │                      │
                  :8080│                 :8081│
                       ▼                      ▼
          ┌────────────────────┐   ┌─────────────────────┐
          │    ms-productos    │   │    ms-inventario    │
          │                   │   │                     │
          │  SecurityConfig   │   │  SecurityConfig     │
          │  ApiKeyFilter     │   │  ApiKeyFilter       │
          │  JwtDecoder ◄─────┼───┼──► JwtDecoder       │
          │                   │   │                     │
          │  [Hex. Arch.]     │   │  [Hex. Arch.]       │
          │  domain/          │◄──┤  WebClient + CB     │ HTTP GET precio
          │  application/     │   │  KafkaConsumer      │◄── product.events
          │  infrastructure/  │   │  OutboxPublisher ───┼──► inventory.events
          └────────┬──────────┘   └──────────┬──────────┘
                   │                          │
            R2DBC  │  Kafka produce    R2DBC  │  Kafka consume
                   ▼                          ▼
          ┌────────────┐  ┌──────────┐  ┌────────────┐
          │ PostgreSQL │  │  Kafka   │  │ PostgreSQL │
          │  :5432     │  │  :9092   │  │  :5433     │
          │            │  │          │  │            │
          │ products   │  │ product  │  │ inventory  │
          │ outbox     │  │ .events  │  │ purchases  │
          └────────────┘  │ inventory│  │ outbox     │
                          │ .events  │  └────────────┘
                          └──────────┘
                               │
          ┌────────────────────▼─────────────────────────┐
          │              Keycloak :8180                   │
          │  KC_HOSTNAME_URL=http://localhost:8180        │
          │  realm=microservicios                         │
          │  → iss del token = http://localhost:8180/...  │
          └───────────────────────────────────────────────┘

          ┌────────────────────────────────────────────────┐
          │             OBSERVABILIDAD                     │
          │  Prometheus :9090  ← scrape /actuator/prometheus│
          │  Grafana    :3000  ← dashboards sobre Prometheus│
          │  Jaeger     :16686 ← trazas OTLP HTTP :4318    │
          └────────────────────────────────────────────────┘
```

### Tópicos Kafka

| Tópico | Productor | Consumidor | Evento |
|---|---|---|---|
| `product.events` | ms-productos (Outbox) | ms-inventario | `ProductCreatedEvent` |
| `inventory.events` | ms-inventario (Outbox) | (futuro ms-notificaciones) | `InventoryUpdatedEvent` |
| `product.events.DLT` | Kafka (auto) | DevOps | Mensajes con 3 fallos |

---

## 5. Flujo de Compra

### Secuencia completa: login → crear producto → stock → comprar

```
Postman          Keycloak         ms-productos      Kafka      ms-inventario
   │                │                  │               │              │
   │─POST /token───►│                  │               │              │
   │  user@test.com │                  │               │              │
   │◄─{access_token}│                  │               │              │
   │  iss=localhost  │                  │               │              │
   │                │                  │               │              │
   │─POST /products ─────────────────►│               │              │
   │  Bearer <jwt>  │   [valida JWT]   │               │              │
   │                │   [ROLE_USER]    │               │              │
   │                │                  │               │              │
   │                │          TX PostgreSQL:           │              │
   │                │          INSERT products          │              │
   │                │          INSERT outbox(false)     │              │
   │                │          COMMIT                   │              │
   │◄─201 Created───────────────────── │               │              │
   │  {data.id}     │                  │               │              │
   │                │                  │               │              │
   │                │    [~5s: OutboxScheduler]         │              │
   │                │          SELECT outbox WHERE published=false     │
   │                │          kafka.send('product.events') ─────────►│
   │                │          UPDATE outbox SET published=true        │
   │                │                  │               │              │
   │                │                  │               │ [Consumer]   │
   │                │                  │               │ deserialize  │
   │                │                  │               │ InitInventory│
   │                │                  │               │ INSERT inv   │
   │                │                  │               │ qty=0        │
   │                │                  │               │ ack() ✓      │
   │                │                  │               │              │
   │─GET /inventory/{id} ──────────────────────────────────────────►│
   │◄─{quantity: 0}─────────────────────────────────────────────────│
   │                │                  │               │              │
   │─PATCH /inventory/{id} ────────────────────────────────────────►│
   │  {quantity:50} │                  │               │              │
   │◄─{quantity: 50}─────────────────────────────────────────────── │
   │                │                  │               │              │
   │─POST /purchase ───────────────────────────────────────────────►│
   │  {productId,   │                  │               │              │
   │   quantity:3}  │         [Circuit Breaker CLOSED] │              │
   │                │                  │◄─GET /products/{id}─────────│
   │                │                  │─{price:1299.99}────────────►│
   │                │                  │               │              │
   │                │                  │       TX PostgreSQL:         │
   │                │                  │       inventory.qty = 47     │
   │                │                  │       INSERT purchase_history│
   │                │                  │       INSERT outbox          │
   │                │                  │       COMMIT                 │
   │◄─201 Created───────────────────────────────────────────────────│
   │  {totalAmount: 3899.97,           │               │              │
   │   remainingStock: 47}             │               │              │
```

### Respuestas de error del endpoint de compra

| Caso | HTTP | Código de error |
|---|---|---|
| Sin autenticación | 401 | `UNAUTHORIZED` |
| Stock insuficiente | 422 | `INSUFFICIENT_STOCK` |
| Producto no existe en inventario | 404 | `INVENTORY_NOT_FOUND` |
| ms-productos no disponible (CB abierto) | 502 | `PRODUCT_SERVICE_UNAVAILABLE` |
| Datos inválidos (quantity ≤ 0) | 400 | `VALIDATION_ERROR` |

### Garantías del sistema

- **Sin pérdida de eventos**: el Outbox Pattern garantiza que si la BD se confirma, el evento llegará a Kafka eventualmente (aunque Kafka esté caído en el momento).
- **Idempotencia del consumer**: si el mismo `ProductCreatedEvent` llega dos veces, `InitializeInventoryUseCase` detecta que el inventario ya existe y retorna el existente sin crear duplicados.
- **At-least-once delivery**: Kafka con `MANUAL_IMMEDIATE` ACK — el consumer solo confirma si el procesamiento fue exitoso. Si falla, reintenta 3 veces antes de enviar al Dead Letter Topic.
- **Consistencia transaccional**: stock descontado + historial de compra + evento outbox en una sola transacción PostgreSQL.

---

## 6. Uso de Herramientas de IA en el Desarrollo

### Herramientas utilizadas

El desarrollo de este proyecto utilizó **Claude (Anthropic)** como asistente de IA principal a lo largo de todo el proceso de construcción.

### Tareas donde se utilizó IA

#### Generación de estructura y boilerplate

Se utilizó IA para generar la estructura inicial de los proyectos con arquitectura hexagonal: árbol de directorios, clases de configuración base, entidades R2DBC, repositorios reactivos y DTOs. El volumen de código repetitivo en arquitectura hexagonal (ports, adapters, mappers) es alto y propenso a inconsistencias — la IA permitió generarlo de forma consistente.

**Verificación aplicada**: cada clase generada fue revisada para confirmar que respetaba las capas (el dominio no importa Spring, los adaptadores no contienen lógica de negocio). Se corrigieron manualmente casos donde la IA colocó anotaciones de Spring (`@Service`, `@Repository`) en clases de dominio.

#### Configuración de infraestructura compleja

La configuración de Keycloak 24 en Docker Compose con auto-import de realm presentó múltiples problemas no documentados: la imagen no incluye `curl` (rompía el healthcheck), el `KC_HOSTNAME_URL` necesario para resolver el split-network Docker/JWT, y el formato específico del `realm-export.json`. Se utilizó IA para iterar rápidamente sobre estas configuraciones.

**Verificación aplicada**: cada configuración fue probada levantando el stack real y verificando el comportamiento esperado (tokens con el issuer correcto, healthcheck en verde, usuarios importados correctamente).

#### Diagnóstico y resolución de bugs

Los bugs más complejos resueltos con asistencia de IA fueron:

| Bug | Causa raíz identificada con IA |
|---|---|
| 401 en todos los requests JWT | `iss` del token (`localhost:8180`) vs issuer configurado (`keycloak:8080`) por split-network Docker |
| `Connection reset` en trazas OTLP | Puerto 4317 es gRPC, Spring Boot usa HTTP en 4318 — protocolo incompatible |
| URL duplicada `/v1/traces/v1/traces` | Variable de entorno ya contenía el path, el `application.yml` lo añadía de nuevo |
| `NoSuchBeanDefinitionException` en tests | `.oauth2ResourceServer()` con `else` sin decoder forzaba a Spring a buscar un bean inexistente |
| NPE en `ProductDomainService` durante tests | `findByName` se llamaba antes de validar el nombre — el mock no estaba configurado para ese caso |
| Kafka `DOWN` en targets de Prometheus | `/actuator/prometheus` no estaba en la lista de rutas públicas del `SecurityConfig` |

**Verificación aplicada**: cada bug fue reproducido antes de aplicar el fix, y verificado que el fix lo resolvía sin introducir regresiones en otros tests o en el comportamiento en producción.

#### Documentación técnica

Se utilizó IA para generar los documentos de arquitectura (`arquitectura_microservicios.docx`), la colección de Postman con 20+ requests y scripts pre-request, y este README. El contenido fue revisado para que refleje con exactitud las decisiones tomadas durante el desarrollo.

#### Pipeline CI/CD

El workflow de GitHub Actions (`.github/workflows/deploy.yml`) fue generado con IA, incluyendo los tres jobs secuenciales (build-check → test → deploy), la sincronización via `rsync`, el health check post-deploy y la configuración del agente SSH.

**Verificación aplicada**: el pipeline fue revisado paso a paso para confirmar que los secrets necesarios están documentados, que el orden de los jobs es correcto y que el health check apunta a los endpoints reales.

### Cómo se verificó la calidad del código generado

1. **Compilación**: todo el código generado fue compilado con `./gradlew compileJava` antes de integrarse. El criterio mínimo es que compile sin errores.

2. **Tests unitarios**: los tests de dominio (`PurchaseDomainServiceTest`, `ProductDomainServiceTest`) verifican el comportamiento del núcleo sin dependencias de infraestructura. Cualquier código de dominio generado por IA debía pasar estos tests.

3. **Tests de integración**: `InventoryControllerIntegrationTest` levanta el contexto de Spring completo con `@SpringBootTest` y verifica los endpoints HTTP con `WebTestClient`. Esto detectó problemas de configuración que los tests unitarios no cubren (como el bug del `ReactiveJwtDecoder` en tests).

4. **Prueba manual end-to-end**: después de cada cambio significativo, se ejecutó el flujo completo (login → crear producto → esperar Kafka → cargar stock → comprar) usando Postman y cURL para verificar el comportamiento real.

5. **Revisión de capas**: se verificó manualmente que el código generado respeta las fronteras de la arquitectura hexagonal — el dominio no importa clases de Spring, los adaptadores no contienen lógica de negocio, los puertos son interfaces puras.

6. **Observabilidad como validación**: Prometheus en `http://localhost:9090/targets` (ambos servicios en UP), Jaeger recibiendo trazas con los servicios correctos, y Grafana mostrando métricas reales fueron usados como indicadores de que la integración completa funciona correctamente.

### Criterio general aplicado

El código generado por IA fue tratado como un punto de partida, no como una solución final. Cada fragmento fue leído, entendido y validado antes de integrarse. Los bugs encontrados durante las pruebas (listados arriba) son evidencia de que la validación fue necesaria — la IA generó soluciones que tenían sentido individualmente pero que requerían ajustes al integrarse con el contexto específico del proyecto.