package cl.duoc.tickets.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Cliente S3.
 *
 * Las credenciales se resuelven con la cadena por defecto de AWS, en este orden:
 *  1. Variables de entorno AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_SESSION_TOKEN
 *     (las que entrega AWS Academy en "AWS Details").
 *  2. El rol IAM asociado a la instancia EC2 (LabInstanceProfile / LabRole en AWS Academy).
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(AppProperties props) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(props.s3().region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (StringUtils.hasText(props.s3().endpoint())) {
            builder.endpointOverride(URI.create(props.s3().endpoint()))
                    .forcePathStyle(true);
        }
        return builder.build();
    }
}
