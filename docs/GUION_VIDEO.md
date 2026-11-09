# Guion del video (Teams)

Duración sugerida: **10 a 12 minutos**. Repartan los bloques para que hablen ambos integrantes.

**Antes de grabar**
- Lab de AWS iniciado y EC2 encendida. Si cambió la IP, actualizar el secret `EC2_HOST` y la variable `baseUrl` de Postman.
- Tener abiertos: Postman (con la colección importada), GitHub (pestaña Actions), Docker Hub, la consola de S3 (el bucket) y una terminal en la EC2 (EC2 → Conectar → EC2 Instance Connect).

---

### 1. Introducción (1 min)
- Presentar el caso: una startup necesita un sistema de reservas de eventos con tickets.
- Explicar la arquitectura con el diagrama del README: Spring Boot, EFS, S3, Docker, GitHub Actions y EC2.

### 2. Pipeline automatizado – criterio 6 (2–3 min)
1. Mostrar `.github/workflows/main.yml` y explicar los pasos: compilar → imagen Docker → Docker Hub → despliegue en EC2.
2. Hacer un cambio pequeño (por ejemplo, un comentario), luego `git push` a `main`.
3. En **Actions**, mostrar todos los pasos en verde.
4. En **Docker Hub**, mostrar la imagen actualizada.
5. En la terminal de la EC2, ejecutar `sudo docker ps` para mostrar el contenedor recién creado. En Postman, el **Health check** debe responder `UP`.

### 3. EFS como almacenamiento temporal – criterio 1 (2 min)
1. En la EC2, ejecutar `df -h | grep efs` para mostrar el EFS montado en `/mnt/efs`.
2. Explicar el `-v /mnt/efs:/app/efs` del pipeline: así el contenedor usa el EFS.
3. En Postman, ejecutar **1. Generar tickets** (los 3) y mostrar `efsPath` en la respuesta.
4. En la EC2, ejecutar `find /mnt/efs/tickets -type f` para mostrar la estructura `evento/usuario/ticket.pdf`.

### 4. Subida a S3 – criterio 2 (1–2 min)
1. En la consola de S3, entrar a `evento123/` → `usuarioX/` → `ticket00X.pdf`.
2. Explicar que la subida es automática al generar el ticket (`uploadedToS3: true`) y mostrar también **Subir ticket a S3**.

### 5. Historial – criterio 5 (1 min)
- En Postman, ejecutar **3. Historial**: todos, por evento, por usuario y por estado.

### 6. Modificar y actualizar en S3 – criterio 3 (2 min)
1. Ejecutar **Cambiar asiento y precio** y mostrar que `fileVersion` sube a 2.
2. En S3, mostrar que cambió la fecha de *Última modificación* del archivo.
3. Ejecutar **Confirmar pago**: el estado pasa de RESERVADO a PAGADO.

### 7. Descargar desde S3 – criterio 4 (1 min)
- Ejecutar **Descargar un ticket** (en Postman: *Send and Download*), abrir el PDF y mostrar el asiento nuevo.
- Ejecutar **Descargar por evento** y **por usuario**, y abrir el ZIP.

### 8. Estadísticas (1 min)
- Ejecutar **6. Estadísticas** y mostrar vendidos, reservados e ingresos del evento.

### 9. Eliminar (30 s)
- Ejecutar **Eliminar ticket** y luego la comprobación (404). Mostrar en S3 que el archivo ya no está.

### 10. Cierre (30 s)
- Repasar cómo se cumplió cada requisito y por qué usar EFS (temporal, compartido) junto a S3 (permanente y organizado).

---

## Checklist de entrega
- [ ] Link del video de Teams
- [ ] Link del repositorio: https://github.com/camiluck/tickets-service
- [ ] ZIP con la documentación
