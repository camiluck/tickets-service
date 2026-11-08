# Guion del video de presentación (Teams)

Duración sugerida: **10 a 15 minutos**. Cada bloque indica qué criterio de la pauta cubre y sus puntos. Reparte los bloques entre los integrantes para que todos hablen.

Antes de grabar:
- Lab de AWS iniciado, EC2 encendida, `EC2_HOST` y la `baseUrl` de Postman actualizados con la IP actual.
- Postman con la colección importada.
- Pestañas abiertas: GitHub (Actions), Docker Hub, consola S3 (bucket), consola EFS, terminal SSH a la EC2.
- Si quieres partir de cero: borra los tickets anteriores (o usa otros `eventId`).

---

### 1. Introducción (≈1 min)
- Integrantes, caso (startup de reservas de eventos con tickets) y arquitectura (diagrama del README).
- Tecnologías: Spring Boot 3, AWS SDK v2, EFS, S3, Docker, GitHub Actions, EC2.

### 2. Pipeline automatizado (criterio 6, 20 pts) (≈2–3 min)
1. Muestra `.github/workflows/main.yml` y explica los pasos.
2. Haz un cambio pequeño en vivo (por ejemplo, un comentario o `info` en `application.properties`), luego `git commit` y `git push` a `main`.
3. En **Actions**, muestra cada paso en verde: tests → imagen → deploy.
4. En **Docker Hub**, muestra la imagen con el tag `latest` y el tag del commit recién publicados.
5. En la terminal de la EC2: `docker ps` (contenedor recién creado), y en Postman **0. Salud** debe responder `UP`.

### 3. EFS como almacenamiento temporal (criterio 1, 15 pts) (≈2 min)
1. EC2: `df -h | grep efs` muestra el EFS montado en `/mnt/efs`.
2. `docker inspect tickets-service | grep -A3 Mounts` (o explica el `-v /mnt/efs:/app/efs` del pipeline).
3. Postman **1. Generar tickets**: ejecuta los 3 requests y muestra en la respuesta `efsPath` y `uploadedToS3: true`.
4. Postman **Ver archivos en EFS**, y en la EC2: `find /mnt/efs/tickets -type f` muestra la estructura `evento/usuario/ticket.pdf`.
5. Muestra que la validación rechaza rutas inválidas (request de error 400).

### 4. Subida a S3 organizada (criterio 2, 10 pts) (≈1–2 min)
1. En la consola S3, navega `evento123/` → `usuarioX/` → `ticket001.pdf`.
2. Postman **2. Subir tickets a S3**: subida manual de un ticket y listados por prefijo de evento y de usuario.
3. Explica que la subida es automática al generar (`S3_AUTO_UPLOAD=true`) y que también existe el endpoint manual.

### 5. Historial (criterio 5, 10 pts) (≈1 min)
- Postman **3. Historial**: completo, por evento, por usuario, por estado y por rango de fechas.

### 6. Modificar y actualizar en S3 (criterio 3, 15 pts) (≈2 min)
1. **Cambiar asiento y precio**: muestra `fileVersion: 2`. En la consola S3 cambia la fecha *Last modified* del objeto; descárgalo y muestra el PDF con el nuevo asiento.
2. **Confirmar pago de reserva**: el estado pasa de RESERVADO a PAGADO.
3. **Transferir a otro usuario**: el archivo desaparece de `usuarioX/` y aparece en `usuarioZ/` (muéstralo en S3).

### 7. Descargar desde S3 (criterio 4, 10 pts) (≈1 min)
- **Descargar PDF de un ticket** (Send and Download): abre el PDF.
- **ZIP por evento** y **ZIP por usuario**: abre el ZIP y muestra la estructura de carpetas.

### 8. Estadísticas (≈1 min)
- **6. Estadísticas**: todos los eventos y `evento123` (vendidos, reservados, ingresos, monto reservado).

### 9. Eliminar (≈30 s)
- **Eliminar ticket**, después la comprobación (404), y muestra que ya no está en S3 ni en el EFS.

### 10. Cierre (≈30 s)
- Repaso rápido de cómo se cumplió cada requisito y por qué EFS (temporal, compartido, baja latencia) + S3 (durable, económico, organizado por llaves).

> Criterio 7 (20 pts): el video debe mostrar **todas** las funciones en vivo y explicar cómo cumple cada requisito. Sigue este orden y no te saltes ninguno.

---

## Checklist de entregables
- [ ] Link del video de Teams
- [ ] Link del repositorio GitHub (público o con acceso al docente)
- [ ] ZIP con la documentación (`README.md`, `docs/`, colección Postman y capturas)
