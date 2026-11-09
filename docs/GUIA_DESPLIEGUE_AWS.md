# Guía de despliegue en AWS Academy

Sigue los pasos en orden. Todo se hace en la región **us-east-1 (N. Virginia)** del Learner Lab.

---

## 1. Bucket S3

1. Consola AWS → **S3** → **Create bucket**.
2. Nombre único, por ejemplo `tickets-cdy2204-grupoX` (minúsculas, sin espacios).
3. Deja **Block all public access** activado: la app accede con credenciales, no por URL pública.
4. **Create bucket**. Anota el nombre: va en el secret `S3_BUCKET`.

> No necesitas crear las carpetas: S3 las genera solas a partir de la llave `evento123/usuarioX/ticket001.pdf`.

## 2. Instancia EC2

**Si no tienes una EC2, créala así:**
1. Consola → **EC2** → **Launch instance**.
2. Nombre: `tickets-ec2`. AMI: **Amazon Linux 2023**. Tipo: **t2.micro** (o t3.micro).
3. **Key pair** → *Create new key pair* → nombre `tickets-key`, tipo RSA, formato `.pem`. Se descarga un archivo: guárdalo bien, porque va en el secret `EC2_SSH_KEY`.
4. **Network settings** → *Edit* → deja la VPC por defecto con *Auto-assign public IP* en **Enable**, crea un security group nuevo (`tickets-sg`) y agrega las reglas del punto 1 de la lista siguiente.
5. **Advanced details** → *IAM instance profile* → `LabInstanceProfile`.
6. **Launch instance**. Cuando quede en estado *Running*, copia la **Public IPv4 address**.
7. Conéctate: botón **Connect → EC2 Instance Connect → Connect** (se abre una terminal en el navegador), o desde tu PC con `ssh -i tickets-key.pem ec2-user@<IP>`.

Luego (o si ya tenías una VM) revisa estos puntos:

1. **Security group de la EC2**, reglas de entrada:
   - `22` (SSH) desde `0.0.0.0/0` (GitHub Actions entra por SSH desde IPs variables)
   - `8080` (TCP personalizado) desde `0.0.0.0/0` (para Postman)
2. **Rol IAM** (recomendado): EC2 → selecciona la instancia → **Actions → Security → Modify IAM role** → `LabInstanceProfile` → **Update**.
   Con esto la app obtiene credenciales para S3 sola y **no tienes que renovar keys cada vez que reinicias el lab**.
3. Instalar Docker y el cliente NFS (Amazon Linux 2023):
   ```bash
   sudo dnf install -y docker nfs-utils
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user
   exit   # vuelve a conectarte para que tome el grupo docker
   ```
   En Ubuntu: `sudo apt update && sudo apt install -y docker.io nfs-common`, y luego `sudo usermod -aG docker ubuntu`.

## 3. Amazon EFS

1. Consola → **EFS** → **Create file system**. Ponle un nombre (`efs-tickets`), elige la **VPC por defecto** y haz clic en **Create**.
2. Entra al file system → pestaña **Network** → **Manage**. En cada zona de disponibilidad cambia el security group por el **mismo de la EC2** y guarda.
3. **Importante:** en ese security group agrega una regla de entrada **NFS (puerto 2049)** cuyo origen sea **el mismo security group**. Sin esta regla el `mount` se queda colgado.
4. Botón **Attach** → copia el DNS (`fs-xxxxxxxx.efs.us-east-1.amazonaws.com`). Va en el secret `EFS_DNS`.
5. Prueba el montaje a mano en la EC2:
   ```bash
   sudo mkdir -p /mnt/efs
   sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
     fs-xxxxxxxx.efs.us-east-1.amazonaws.com:/ /mnt/efs
   df -h | grep efs
   ```
6. (Opcional) Montaje permanente tras reinicios, agregando a `/etc/fstab`:
   ```
   fs-xxxxxxxx.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0
   ```
   De todas formas, el pipeline monta el EFS si no está montado.

## 4. Docker Hub

1. Crea una cuenta en https://hub.docker.com (si no tienes).
2. **Account settings → Personal access tokens → Generate new token** con permiso *Read & Write*. Ese token va en `DOCKERHUB_TOKEN`.
3. El repositorio `tickets-service` se crea solo en el primer push.

## 5. Secrets de GitHub

Repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | Tu usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Token del paso 4 |
| `EC2_HOST` | IP pública (o DNS público) de la EC2 |
| `EC2_USER` | `ec2-user` (Amazon Linux) o `ubuntu` |
| `EC2_SSH_KEY` | Contenido completo del `.pem` (incluyendo las líneas `-----BEGIN…` y `-----END…`) |
| `EFS_DNS` | `fs-xxxxxxxx.efs.us-east-1.amazonaws.com` |
| `S3_BUCKET` | Nombre del bucket |

> En AWS Academy la IP pública de la EC2 cambia cada vez que se reinicia el lab: actualiza `EC2_HOST` y la `baseUrl` de Postman.

## 6. Primer despliegue

```bash
git init
git add .
git commit -m "Sistema de tickets con EFS, S3 y CI/CD"
git branch -M main
git remote add origin https://github.com/<usuario>/<repo>.git
git push -u origin main
```

En la pestaña **Actions** del repo verás los pasos: checkout → JDK → `mvn package` → login → build/push → deploy en EC2.

Para comprobarlo:
```bash
curl http://<IP-EC2>:8080/actuator/health
```

## 7. Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `mount` se queda colgado | Falta la regla NFS 2049 en el security group del EFS, o el EFS no está en el mismo SG/VPC que la EC2 |
| `500` con "Error al subir a S3 … Unable to load credentials" | La EC2 no tiene el rol `LabInstanceProfile` |
| `500` con "Access Denied" o "NoSuchBucket" | Nombre de bucket mal escrito en `S3_BUCKET`, o el bucket está en otra región |
| `ssh: handshake failed` en Actions | `EC2_SSH_KEY` incompleto, `EC2_USER` incorrecto o puerto 22 cerrado |
| `permission denied … docker.sock` | El usuario no está en el grupo `docker` (paso 2.3) |
| Postman no conecta | Puerto 8080 cerrado en el SG, o la IP cambió |
| Ver logs | `docker logs -f tickets-service` en la EC2 |
