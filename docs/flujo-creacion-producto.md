# Flujo: Creación de producto → Stock inicializado en 0

## Decisión de diseño

Cuando se crea un producto, el inventario se inicializa automáticamente en 0
mediante un flujo **event-driven asíncrono** usando el patrón **Outbox + Kafka**.

### ¿Por qué asíncrono y no síncrono (HTTP)?

| Opción | Problema |
|--------|----------|
| ms-productos llama HTTP a ms-inventario al crear | Acoplamiento temporal: si inventario está caído, la creación del producto falla |
| ms-inventario consulta productos al arrancar | No escala, no es reactivo |
| **Outbox Pattern + Kafka** ✅ | Desacoplado, consistente, tolerante a fallos |

## Flujo completo paso a paso

```
Cliente
  │
  │  POST /api/v1/products  {"name":"Laptop","price":1299.99}
  ▼
ms-productos (CreateProductCommandHandler)
  │
  ├─[1] INSERT INTO products (...)          ┐
  ├─[2] INSERT INTO outbox_events (         ├─ UNA SOLA TRANSACCIÓN R2DBC
  │       event_type = 'ProductCreatedEvent'│  Si algo falla → rollback de todo
  │       payload    = { productId, name, price, minStock: 0 }
  │       published  = false               ┘
  │
  │  HTTP 201 Created ← responde inmediatamente al cliente
  │
  ▼  (5 segundos después — scheduler)
ProductEventPublisher
  ├─ SELECT * FROM outbox_events WHERE published = false
  ├─ KafkaTemplate.send("product.events", productId, payload)
  └─ UPDATE outbox_events SET published = true WHERE id = ?

  ─────── Kafka topic: product.events ───────────────────────────────────────

ms-inventario (ProductCreatedEventConsumer)
  ├─[3] Deserializa ProductCreatedEventDto
  ├─[4] InitializeInventoryUseCase.execute(productId, minStock=0)
  │       ├─ findByProductId → vacío (nuevo)
  │       └─ INSERT INTO inventory (product_id, quantity=0, min_stock=0)
  └─[5] ack.acknowledge() → offset Kafka avanza
```

## Garantías

| Garantía | Mecanismo |
|----------|-----------|
| **No se pierde el evento** si Kafka está caído | Outbox en PostgreSQL — el evento persiste en DB hasta publicarse |
| **No se duplica el inventario** si el evento se re-procesa | `InitializeInventoryUseCase` es idempotente: si ya existe, ignora |
| **No hay stock fantasma** si falla la creación del producto | Transacción: producto + outbox_event en el mismo commit |
| **Retry automático** si ms-inventario falla al procesar | `DefaultErrorHandler`: 3 reintentos × 2 segundos |
| **Auditoría de fallos** | Dead Letter Topic `product.events.DLT` |

## Latencia esperada

- El cliente recibe `201 Created` en ~20-50ms
- El stock aparece en ms-inventario ~5-10 segundos después (outbox poll interval)
- En producción se puede reducir a 1 segundo ajustando `app.kafka.outbox-poll-ms`

## Cómo verificar el flujo localmente

```bash
# 1. Crear producto
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "X-Service-Api-Key: dev-secret-productos-key" \
  -d '{"name":"Laptop Pro","price":1299.99}' | jq -r '.data.id')

echo "Producto creado: $PRODUCT_ID"

# 2. Esperar ~5 segundos y consultar inventario
sleep 6

curl -s http://localhost:8081/api/v1/inventory/$PRODUCT_ID \
  -H "X-Service-Api-Key: dev-secret-inventario-key" | jq .

# Respuesta esperada:
# {
#   "data": {
#     "type": "inventories",
#     "attributes": {
#       "productId": "...",
#       "productName": "Laptop Pro",
#       "quantity": 0,        ← stock inicializado en 0
#       "minStock": 0,
#       "lowStock": false
#     }
#   }
# }
```
