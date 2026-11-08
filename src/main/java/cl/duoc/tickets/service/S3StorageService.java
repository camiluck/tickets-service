package cl.duoc.tickets.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.duoc.tickets.config.AppProperties;
import cl.duoc.tickets.dto.S3ObjectInfo;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Operaciones contra AWS S3. Las llaves siguen la estructura {eventId}/{userId}/{ticketCode}.pdf
 * (ej: evento123/usuarioX/ticket001.pdf).
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, AppProperties props) {
        this.s3 = s3;
        this.bucket = props.s3().bucket();
    }

    public void upload(String key, byte[] content, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(content));
            log.info("Subido a S3: s3://{}/{}", bucket, key);
        } catch (SdkException e) {
            throw new StorageException("Error al subir a S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    public byte[] download(String key) {
        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket())
                    .key(key)
                    .build());
            return bytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("No existe en S3: " + key);
        } catch (SdkException e) {
            throw new StorageException("Error al descargar de S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(key).build());
            log.info("Eliminado de S3: s3://{}/{}", bucket, key);
        } catch (SdkException e) {
            throw new StorageException("Error al eliminar de S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    /** Lista los objetos del bucket bajo un prefijo (ej: "evento123/" o "evento123/usuarioX/"). */
    public List<S3ObjectInfo> list(String prefix) {
        List<S3ObjectInfo> result = new ArrayList<>();
        String token = null;
        try {
            do {
                ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket());
                if (StringUtils.hasText(prefix)) {
                    req.prefix(prefix);
                }
                if (token != null) {
                    req.continuationToken(token);
                }
                ListObjectsV2Response resp = s3.listObjectsV2(req.build());
                resp.contents().forEach(o -> result.add(new S3ObjectInfo(o.key(), o.size(), o.lastModified())));
                token = Boolean.TRUE.equals(resp.isTruncated()) ? resp.nextContinuationToken() : null;
            } while (token != null);
            return result;
        } catch (SdkException e) {
            throw new StorageException("Error al listar S3: " + e.getMessage(), e);
        }
    }

    public String bucketName() {
        return bucket;
    }

    private String bucket() {
        if (!StringUtils.hasText(bucket)) {
            throw new StorageException("No se configuro el bucket S3 (variable de entorno S3_BUCKET)");
        }
        return bucket;
    }
}
