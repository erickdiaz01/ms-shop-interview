# Microservicios: ms-productos + ms-inventario

## Inicio Rápido

```bash
# 1. Levantar todo el stack (incluye Keycloak)
docker compose up --build -d

# 2. Esperar que todos los servicios esten saludables (~2-3 minutos)
docker compose ps

# 3. Verificar health
curl http://localhost:8080/actuator/health   # UP
curl http://localhost:8081/actuator/health   # UP
curl http://localhost:8180/health/ready      # UP (Keycloak)
```

---

## Mapa de Servicios y Puertos

| Servicio | URL | Descripción |
|----------|-----|-------------|
| ms-productos | http://localhost:8080 | API de catálogo de productos |
| ms-inventario | http://localhost:8081 | API de inventario y compras |
| Swagger productos | http://localhost:8080/swagger-ui.html | Documentación interactiva |
| Swagger inventario | http://localhost:8081/swagger-ui.html | Documentación interactiva |
| **Keycloak** | **http://localhost:8180** | **Identity Provider (admin/admin)** |
| Kafka UI | http://localhost:8090 | Explorar topics y mensajes |
| Prometheus | http://localhost:9090 | Métricas |
| Grafana | http://localhost:3000 | Dashboards (admin/admin) |
| Jaeger | http://localhost:16686 | Trazas distribuidas |

---

## Autenticación

Los servicios aceptan DOS formas de autenticación:

### Opción A: JWT via Keycloak (clientes externos / Postman)

```bash
# 1. Obtener token
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/microservicios/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=ms-client' \
  -d 'client_secret=ms-client-secret' \
  -d 'username=user@test.com' \
  -d 'password=test123' \
  | jq -r '.access_token')

# 2. Usar en peticiones
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

**Usuarios disponibles:**

| Username | Password | Roles |
|----------|----------|-------|
| user@test.com | test123 | ROLE_USER |
| admin@test.com | admin123 | ROLE_ADMIN, ROLE_USER |
| service@test.com | service123 | ROLE_SERVICE |

### Opción B: API Key (service-to-service / desarrollo rápido)

```bash
curl http://localhost:8080/api/v1/products \
  -H "X-Service-Api-Key: dev-secret-productos-key"

curl http://localhost:8081/api/v1/inventory/purchase \
  -H "X-Service-Api-Key: dev-secret-inventario-key"
```

---

## Credenciales Bases de Datos

| BD | Host externo | User | Password |
|----|-------------|------|----------|
| productos_db | localhost:5432 | productos_user | productos_pass |
| inventario_db | localhost:5433 | inventario_user | inventario_pass |

```bash
# Conectar con psql:
docker exec -it postgres-productos psql -U productos_user -d productos_db
docker exec -it postgres-inventario psql -U inventario_user -d inventario_db
```

---

## Postman

Importar ambos archivos desde la carpeta `postman/`:
- `microservicios-collection.json` — todas las requests
- `local-docker-environment.json` — variables de entorno

Ver `postman/README.md` para instrucciones detalladas.

---

## Flujo End-to-End (curl)

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8180/realms/microservicios/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=ms-client&client_secret=ms-client-secret&username=user@test.com&password=test123' \
  | jq -r '.access_token')

# Crear producto
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Laptop Pro 15","price":1299.99}' | jq -r '.data.id')

sleep 6  # Kafka event processing

# Ver stock (debe ser 0)
curl -s http://localhost:8081/api/v1/inventory/$PRODUCT_ID \
  -H "Authorization: Bearer $TOKEN" | jq .data.attributes

# Cargar stock
curl -s -X PATCH http://localhost:8081/api/v1/inventory/$PRODUCT_ID \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"quantity":50}'

# Comprar
curl -s -X POST http://localhost:8081/api/v1/inventory/purchase \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":3}" | jq .
```

---

## Tests

```bash
cd ms-productos && mvn test
cd ms-inventario && mvn test

# Con cobertura JaCoCo:
mvn verify && open target/site/jacoco/index.html
```

---

## EKS Deploy

```bash
./scripts/build-and-push.sh 1.0.0
helm upgrade --install ms-productos ./helm/ms-productos --namespace ms-inventory --create-namespace
helm upgrade --install ms-inventario ./helm/ms-inventario --namespace ms-inventory
```
