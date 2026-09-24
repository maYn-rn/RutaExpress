# RutaExpress

Sistema de envios con frontend React autenticado con Azure AD (MSAL), un BFF
en Spring Boot que valida el JWT y tres microservicios desplegados en AWS
(EC2 + API Gateway + RDS MySQL).

```
React (MSAL) ──JWT──▶ AWS API Gateway (JWT authorizer)
                          │
                          ▼
                     EC2: ms-rutaexpress-bff :8080  (valida issuer, audience, firma, exp y roles)
                          ├──▶ ms-rutaexpress-catalog   :8081 ──▶ RDS MySQL (catalog_db)
                          └──▶ ms-rutaexpress-shipments :8082 ──▶ RDS MySQL (shipments_db)
```

## Estructura

| Carpeta / archivo | Contenido |
|---|---|
| `frontend-rutaexpress/` | React + `@azure/msal-react`. Login/logout, token en cada llamada, UI segun roles |
| `ms-rutaexpress-bff/` | Backend-For-Frontend. Resource server OAuth2 + proxy a los microservicios |
| `ms-rutaexpress-catalog/` | CRUD de servicios de envio (JPA + MySQL) |
| `ms-rutaexpress-shipments/` | CRUD de envios con maquina de estados (JPA + MySQL) |
| `docker-compose.yml` | Entorno local (incluye MySQL en Docker) |
| `docker-compose.aws.yml` | Despliegue en EC2 (usa Amazon RDS, sin MySQL local) |
| `.env.aws.example` | Variables necesarias para el despliegue en AWS |

## Seguridad

- **Azure AD App roles**: `Admin` y `Operador`. Scope expuesto: `access_as_user`.
- **BFF** (`AccessRules`):

  | Operacion | Requisito |
  |---|---|
  | GET (leer) | scope `access_as_user` |
  | POST / PUT (crear, modificar) | rol `Admin` u `Operador` |
  | DELETE | rol `Admin` |

  Respuestas: `401` token ausente/invalido/expirado o audience incorrecto,
  `403` sin rol/scope, `503` si un microservicio no responde. `GET /api/me`
  devuelve los roles, scopes y authorities que el BFF extrajo del token.
- **API Gateway**: JWT authorizer con el mismo issuer y audience que el BFF.
- **Frontend**: lee `roles` y `scp` desde los claims del token (pestana
  "Mi sesion") y oculta las acciones que el usuario no puede realizar.

## Ejecutar en local

Requisitos: Docker Desktop y Node.js.

```powershell
docker compose up --build          # mysql, catalog, shipments y bff (8080)

cd frontend-rutaexpress
copy .env.example .env             # completar con los datos de Azure
npm install
npm start                          # http://localhost:3000
```

## Desplegar en AWS (Learner Lab)

1. **EC2** Amazon Linux 2023, `t3.small`, security group con 22 y 8080 abiertos, Elastic IP asociada.
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
   cp .env.aws.example .env && nano .env    # endpoint y clave de RDS
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
