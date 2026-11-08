package cl.duoc.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades propias de la aplicacion (prefijo "app" en application.properties).
 *
 * @param efs configuracion del almacenamiento temporal en EFS
 * @param s3  configuracion del bucket S3
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Efs efs, S3 s3) {

    /**
     * @param basePath carpeta donde esta montado el EFS dentro del contenedor (ej: /app/efs)
     */
    public record Efs(String basePath) {
    }

    /**
     * @param bucket     nombre del bucket S3
     * @param region     region AWS (us-east-1 en AWS Academy)
     * @param autoUpload si es true, cada ticket generado/modificado se sube automaticamente a S3
     * @param endpoint   endpoint alternativo (solo para pruebas locales con un S3 simulado). Vacio = AWS real
     */
    public record S3(String bucket, String region, boolean autoUpload, String endpoint) {
    }
}
