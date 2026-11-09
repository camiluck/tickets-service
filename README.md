# Sistema de Gestión de Reservas de Eventos con Tickets

**Desarrollo Cloud Native (CDY2204) – Semana 3: "Construyendo y desplegando una solución Cloud Native"**

Microservicio Spring Boot que genera tickets en PDF, los guarda temporalmente en **Amazon EFS**, los sube a **Amazon S3** organizados por evento y usuario, y se despliega automáticamente en **EC2** con **GitHub Actions** y **Docker Hub**.

## Arquitectura

```
Postman ──► EC2 (contenedor Docker :8080)
                 │
                 ├── /app/efs  ◄── montado desde /mnt/efs ──► Amazon EFS  (PDF temporales + BD H2)
                 │
                 └── AWS SDK ──────────────────────────────► Amazon S3   (evento/usuario/ticket.pdf)

git push a main ──► GitHub Actions: mvn package ► docker build/push (Docker Hub) ► SSH a EC2 ► docker run
```

**Flujo de un ticket**
1. `POST /api/tickets` guarda el ticket y genera el PDF en el EFS: `/app/efs/tickets/evento123/usuarioX/ticket001.pdf`
2. Luego se sube automáticamente a S3 con la llave `evento123/usuarioX/ticket001.pdf`
3. Al modificarlo, se vuelve a generar el PDF (sube `fileVersion`) y se reemplaza en S3
4. Al eliminarlo, se borra de S3, del EFS y de la base de datos

## Endpoints

| Requisito | Método | Ruta |
|---|---|---|
| Generar tickets | POST | `/api/tickets` |
| Subir tickets a S3 | POST | `/api/tickets/{id}/upload` |
| Descargar un ticket | GET | `/api/tickets/{id}/download` |
| Descargar por usuario o evento (ZIP) | GET | `/api/tickets/download?eventId=...&userId=...` |
| Modificar detalles del ticket | PUT | `/api/tickets/{id}` |
| Eliminar tickets específicos | DELETE | `/api/tickets/{id}` |
| Estadísticas de ventas y reservas por evento | GET | `/api/stats/events` y `/api/stats/events/{eventId}` |
| Historial de tickets | GET | `/api/tickets?eventId=...&userId=...&status=...` |

Estados: `RESERVADO` (reserva), `PAGADO` (venta), `CANCELADO`.

Ejemplo para generar un ticket:
```json
{
  "eventId": "evento123",
  "eventName": "Concierto Rock 2026",
  "eventDate": "2026-12-15",
  "venue": "Movistar Arena",
  "userId": "usuarioX",
  "userName": "Juan Perez",
  "userEmail": "juan.perez@mail.com",
  "seat": "A-10",
  "price": 35000,
  "status": "PAGADO"
}
```

La colección de Postman está en `postman/Tickets-Service.postman_collection.json`.

## Estructura del código

```
src/main/java/cl/duoc/tickets
├── config/S3Config.java            # Cliente de S3
├── model/Ticket.java               # Entidad
├── repository/TicketRepository.java
├── dto/                            # Datos de entrada y salida
├── service/
│   ├── EfsStorageService.java      # Guardar/leer/eliminar en EFS
│   ├── S3StorageService.java       # Subir/descargar/eliminar en S3
│   ├── PdfTicketGenerator.java     # Genera el PDF
│   ├── TicketService.java          # Lógica de los tickets
│   └── StatsService.java           # Estadísticas
└── web/                            # Controladores REST
```

## Pipeline (`.github/workflows/main.yml`)

Al hacer push a `main`:
1. Compila con Maven (`mvn package`)
2. Construye la imagen Docker y la publica en Docker Hub
3. Se conecta por SSH a la EC2, monta el EFS y ejecuta:
   `docker run -d -p 8080:8080 -v /mnt/efs:/app/efs -e S3_BUCKET=... usuario/tickets-service:latest`

Secrets usados: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `EFS_DNS`, `S3_BUCKET`.

La EC2 tiene asignado el rol **LabRole**, por eso la app accede a S3 sin escribir credenciales en el código.

Guía de configuración de AWS: `docs/GUIA_DESPLIEGUE_AWS.md` · Guion del video: `docs/GUION_VIDEO.md`
