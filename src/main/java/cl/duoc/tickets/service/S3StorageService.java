package cl.duoc.tickets.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Operaciones contra AWS S3.
 * Las llaves tienen la forma {eventId}/{userId}/{ticketCode}.pdf
 * (por ejemplo: evento123/usuarioX/ticket001.pdf).
 */
@Service
public class S3StorageService {

    private final S3Client s3;

    @Value("${s3.bucket}")
    private String bucket;

    public S3StorageService(S3Client s3) {
        this.s3 = s3;
    }

    /** Sube (o reemplaza si ya existe) un archivo en S3. */
    public void subir(String key, byte[] contenido) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .build();
            s3.putObject(request, RequestBody.fromBytes(contenido));
        } catch (Exception e) {
            throw new StorageException("Error al subir a S3: " + e.getMessage());
        }
    }

    public byte[] descargar(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return s3.getObjectAsBytes(request).asByteArray();
        } catch (Exception e) {
            throw new StorageException("Error al descargar de S3: " + e.getMessage());
        }
    }

    public void eliminar(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3.deleteObject(request);
        } catch (Exception e) {
            throw new StorageException("Error al eliminar de S3: " + e.getMessage());
        }
    }
}
