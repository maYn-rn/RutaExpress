# RutaExpress

Sistema de envios con frontend React autenticado con Azure AD (MSAL), un BFF
en Spring Boot que valida el JWT y microservicios desplegados en AWS
(EC2 + API Gateway + RDS MySQL), con mensajeria asincrona en RabbitMQ.

```
React (MSAL) ──JWT──▶ AWS API Gateway (JWT authorizer)
                          │
                          ▼
                     EC2: ms-rutaexpress-bff :8080  (valida issuer, audience, firma, exp y roles)
                          ├──▶ ms-rutaexpress-catalog     :8081 ──▶ RDS MySQL (catalog_db)
                          ├──▶ ms-rutaexpress-shipments   :8082 ──▶ RDS MySQL (shipments_db)
                          │         └─ publica comandos ──▶ RabbitMQ ──▶ ms-rutaexpress-notifications
                          ├──▶ ms-rutaexpress-notifications :8083 ──▶ RDS MySQL (notifications_db)
                          └──▶ ms-rutaexpress-rabbit-admin :8084 ──▶ RabbitMQ (colas, exchanges, bindings)
```

## Estructura

| Carpeta / archivo | Contenido |
|---|---|
| `frontend-rutaexpress/` | React + `@azure/msal-react`. Login/logout, token en cada llamada, UI segun roles |
| `ms-rutaexpress-bff/` | Backend-For-Frontend. Resource server OAuth2 + proxy a los microservicios |
| `ms-rutaexpress-catalog/` | CRUD de servicios de envio (JPA + MySQL) |
| `ms-rutaexpress-shipments/` | CRUD de envios con maquina de estados (JPA + MySQL). **Productor** RabbitMQ |
| `ms-rutaexpress-notifications/` | **Consumidores** RabbitMQ (email, bodega, etiqueta y DLQ, ACK manual) + API de notificaciones y metricas (JPA + MySQL) |
| `ms-rutaexpress-rabbit-admin/` | API REST para crear/eliminar colas, exchanges y bindings |
| `docker-compose.yml` | Entorno local (incluye MySQL y RabbitMQ en Docker) |
| `docker-compose.aws.yml` | Despliegue en EC2 (usa Amazon RDS; RabbitMQ en la misma EC2) |
| `.env.aws.example` | Variables necesarias para el despliegue en AWS |

## Seguridad

- **Azure AD App roles**: `Admin`, `Operador` y `Cliente`. Scope expuesto: `access_as_user`.
- **BFF** (`AccessRules`):

  | Operacion | Requisito |
  |---|---|
  | GET (leer) | scope `access_as_user` |
  | POST envios (solicitar envio) | rol `Admin`, `Operador` o `Cliente` |
  | POST / PUT catalogo, PUT estado de envio | rol `Admin` u `Operador` |
  | DELETE | rol `Admin` |
  | `/api/admin/rabbit/**` (administrar RabbitMQ) | rol `Admin` |
  | `GET /api/messaging/metrics` (tasa de DLQ) | rol `Admin` u `Operador` |
  | `GET /api/notifications/me`, `PUT /api/notifications/{id}/leida` | scope `access_as_user` (solo las propias) |

  Solo `Admin` y `Operador` ven todos los envios. Cualquier otro usuario
  (`Cliente` o sin rol asignado) ve y crea unicamente envios a su nombre: el BFF toma el email del token (`email` / `preferred_username`) y
  lo usa para filtrar `GET /api/shipments`, para forzar `clienteEmail` en
  `POST /api/shipments` y para responder `404` ante un envio ajeno.

  Respuestas: `401` token ausente/invalido/expirado o audience incorrecto,
  `403` sin rol/scope, `503` si un microservicio no responde. `GET /api/me`
  devuelve los roles, scopes y authorities que el BFF extrajo del token.
- **API Gateway**: JWT authorizer con el mismo issuer y audience que el BFF.
- **Frontend**: lee `roles` y `scp` desde los claims del token. `Admin` y
  `Operador` ven las pestanas Catalogo, Envios y "Mi sesion", con las acciones
  que su rol permite. Cualquier otro usuario ve solo **"Mis envios"**: sus
  envios con el estado actual (y el motivo si la entrega fallo o se cancelo),
  la campana de avisos y un aviso emergente cuando un envio cambia de estado
  (los avisos se consultan cada 15 s).

## Mensajeria (RabbitMQ)

Los nombres de colas, exchanges y routing keys estan centralizados en el
`application.yml` de cada servicio (`app.messaging.*`) y los beans se declaran
en una clase de configuracion por servicio (`RabbitProducerConfig`,
`RabbitMessagingConfig`).

### Topologia (6 colas: 3 flujos + 3 DLQ)

Exchanges: `cmd.direct` (direct), `cmd.topic` (topic) y `cmd.dead.dlx` (direct, para DLQ).

| Cola principal | Proposito | DLQ | Binding direct | Binding topic |
|---|---|---|---|---|
| `q.cmd.email` | Email al cliente (registrado, aceptado, en ruta, entregado, fallido, cancelado) | `q.cmd.email.dlq` | `email.send` | `email.*` |
| `q.cmd.warehouse` | Ticket de picking en bodega | `q.cmd.warehouse.dlq` | `warehouse.ticket` | `warehouse.#` |
| `q.cmd.label` | Guia de despacho / etiqueta | `q.cmd.label.dlq` | `label.gen` | `label.*` |

### Que publica shipments

| Hecho | Exchange | Routing key | Cola |
|---|---|---|---|
| Envio creado | `cmd.direct` | `email.send` | `q.cmd.email` |
| Envio creado | `cmd.direct` | `label.gen` | `q.cmd.label` |
| Cambio de estado | `cmd.topic` | `email.<estado>` (ej. `email.entregado`) | `q.cmd.email` |
| Cambio a `ACEPTADO` | `cmd.direct` | `warehouse.ticket` | `q.cmd.warehouse` |

La publicacion ocurre despues del commit y un fallo de RabbitMQ solo queda en
el log: la logica de envios no se ve afectada.

Estados del envio: `CREADO → ACEPTADO → EN_TRANSITO → ENTREGADO`, con
`EN_TRANSITO → FALLIDO → EN_TRANSITO` (reintento de entrega) y `CANCELADO`
desde cualquier estado no final. Al pasar a `FALLIDO` o `CANCELADO` se puede
enviar un `motivo` que se incluye en el aviso al cliente.

### Envelope comun

```json
{
  "type": "SHIPMENT_STATUS_CHANGED",
  "eventId": "6f1c...",            // unico por mensaje -> idempotencia
  "timestamp": "2026-10-30T15:04:05Z",
  "traceId": "a2b9...",            // comun a los mensajes de una misma operacion
  "correlationId": "shipment-7",   // comun a todos los mensajes de un envio
  "data": { "shipmentId": 7, "clienteEmail": "...", "estadoAnterior": "EN_TRANSITO", "estado": "FALLIDO", "motivo": "...", ... }
}
```

`eventId`, `correlationId`, `type` y `x-trace-id` tambien viajan como
propiedades AMQP (visibles en el panel de RabbitMQ).

### Confirmacion de mensajes (`acknowledge-mode: manual`)

| Situacion | Accion | Destino |
|---|---|---|
| Procesado | `basicAck` | se elimina |
| Duplicado (`eventId` ya procesado por ese consumidor) | `basicAck` sin reprocesar | se elimina |
| JSON invalido o datos faltantes | `basicReject(requeue=false)` | DLQ |
| Error recuperable con intentos disponibles | reenvio con `x-retry-count + 1` y `basicAck` | misma cola |
| Error recuperable tras 3 intentos | `basicNack(requeue=false)` | DLQ |
| Error inesperado | `basicNack(requeue=false)` | DLQ |

La idempotencia se guarda en la tabla `processed_messages` (consumidor + `eventId`).

### Como se entera el cliente

1. Cada mensaje de `q.cmd.email` envia el email y guarda una notificacion
   `ENVIADA` (tabla `notifications`).
2. Si el email no sale tras los reintentos, el mensaje llega a
   `q.cmd.email.dlq` y `DeadLetterListener` guarda la notificacion como
   `FALLIDA` (con el motivo): el cliente igual ve el cambio en la app.
3. El cliente consulta sus avisos con `GET /api/notifications/me` (el email
   sale de su token) y los marca como leidos con `PUT /api/notifications/{id}/leida`.
4. Toda DLQ queda registrada en `dead_letters`; `GET /api/messaging/metrics`
   devuelve por cola los procesados, los enviados a DLQ y la tasa de DLQ (%).

**Demo:** crear un envio cuyo cliente contenga `#fallo` provoca 3 reintentos
del email, su paso a `q.cmd.email.dlq` y una notificacion `FALLIDA` (la guia
de despacho se genera igual). Publicar un texto que no sea JSON desde el panel
de RabbitMQ provoca un REJECT directo a la DLQ.

### API del administrador (via BFF: `/api/admin/rabbit`, rol Admin)

| Metodo | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `/queues` | `{"name","durable?","deadLetterExchange?","deadLetterRoutingKey?","messageTtlMs?"}` | 201 / 400 / 409 |
| GET | `/queues/{name}` | | 200 / 404 |
| DELETE | `/queues/{name}` | | 204 / 404 |
| POST | `/exchanges` | `{"name","type":"direct\|topic\|fanout","durable?"}` | 201 / 400 / 409 |
| DELETE | `/exchanges/{name}` | | 204 |
| POST | `/bindings` | `{"exchange","queue","routingKey"}` | 201 / 400 / 404 |
| DELETE | `/bindings` | `{"exchange","queue","routingKey"}` | 204 |

Los nombres solo admiten letras, numeros, `.`, `-` y `_`, y no pueden empezar
con `amq.` (reservado). Toda la logica esta en `RabbitAdminService`.

## Ejecutar en local

Requisitos: Docker Desktop y Node.js.

```powershell
docker compose up --build          # mysql, rabbitmq, 5 microservicios; bff en 8080
                                   # panel RabbitMQ: http://localhost:15672 (admin / admin123)

cd frontend-rutaexpress
copy .env.example .env             # completar con los datos de Azure
npm install
npm start                          # http://localhost:3000
```

## Desplegar en AWS (Learner Lab)

1. **EC2** Amazon Linux 2023, `t3.medium` (o `t3.small` con 2 GB de swap), security group con 22 y 8080
   abiertos y 15672 (panel de RabbitMQ) solo para "Mi IP", Elastic IP asociada.
2. **RDS** MySQL 8 `db.t3.micro` con "Connect to an EC2 compute resource" apuntando a la EC2
   (Enhanced monitoring y Performance Insights desactivados).
3. En la EC2:
   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user   # salir y volver a conectarse
   mkdir -p ~/.docker/cli-plugins
   curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o ~/.docker/cli-plugins/docker-compose
   curl -SL https://github.com/docker/buildx/releases/download/v0.19.3/buildx-v0.19.3.linux-amd64 -o ~/.docker/cli-plugins/docker-buildx
   chmod +x ~/.docker/cli-plugins/*

   git clone <URL_DE_ESTE_REPO> rutaexpress && cd rutaexpress
   cp .env.aws.example .env && nano .env    # endpoint y clave de RDS, clave de RabbitMQ
   docker compose -f docker-compose.aws.yml up -d --build
   curl -i http://localhost:8080/api/me     # 401 = BFF arriba y protegido
   ```
4. **API Gateway (HTTP API)**: ruta `ANY /api/{proxy+}` → `http://<ELASTIC_IP>:8080/api/{proxy}`,
   JWT authorizer (issuer `https://login.microsoftonline.com/<TENANT_ID>/v2.0`, audience `<CLIENT_ID>`,
   scope `access_as_user`) y CORS para `http://localhost:3000`.
5. En `frontend-rutaexpress/.env`: `REACT_APP_BFF_URL=<Invoke URL del API Gateway>`.

## Tests

```powershell
# Backend (en cada microservicio, requiere JDK 21 + Maven o Docker)
mvn test

# Frontend
cd frontend-rutaexpress
npm test
```
