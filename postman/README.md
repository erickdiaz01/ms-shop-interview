# Colección Postman — ms-productos + ms-inventario

## Archivos
- `microservicios-collection.json` — Colección con todas las requests
- `local-docker-environment.json` — Variables de entorno para local

## Importar en Postman

1. Abrir Postman
2. `File > Import` (o arrastrar los archivos)
3. Importar AMBOS archivos: la colección y el environment
4. En la esquina superior derecha: seleccionar environment `Local Docker`

## Usuarios disponibles

| Usuario | Password | Roles | Usar para |
|---------|----------|-------|-----------|
| user@test.com | test123 | ROLE_USER | Pruebas normales |
| admin@test.com | admin123 | ROLE_ADMIN, ROLE_USER | Pruebas admin |
| service@test.com | service123 | ROLE_SERVICE | Pruebas service-to-service |

## Flujo recomendado

1. Levantar stack: `docker compose up --build -d`
2. Esperar que Keycloak esté healthy (~60s): `docker compose ps keycloak`
3. En Postman: ejecutar **Step 1: Login** (guarda el token automáticamente)
4. Ejecutar las requests en orden o usar **Flujo Completo**

## Configurar OAuth2 en Postman (Authorization Code)

Alternativa al Resource Owner Password para flujos de browser:

1. En la colección > Authorization > Type: OAuth 2.0
2. Configure New Token:
   - Token Name: `keycloak-token`
   - Grant Type: Authorization Code (with PKCE)
   - Auth URL: `http://localhost:8180/realms/microservicios/protocol/openid-connect/auth`
   - Access Token URL: `http://localhost:8180/realms/microservicios/protocol/openid-connect/token`
   - Client ID: `postman-client` (cliente público, sin secret)
   - Scope: `openid profile email`
3. Click **Get New Access Token** → abre browser → login → token listo
