package cl.duoc.tickets.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.tickets.config.AppProperties;
import cl.duoc.tickets.dto.EfsFileInfo;
import jakarta.annotation.PostConstruct;

/**
 * Almacenamiento temporal en Amazon EFS.
 *
 * El EFS esta montado en la EC2 en /mnt/efs y el contenedor lo ve en /app/efs
 * (docker run -v /mnt/efs:/app/efs ...). Los PDF se guardan en:
 *   /app/efs/tickets/{eventId}/{userId}/{ticketCode}.pdf
 */
@Service
public class EfsStorageService {

    private static final Logger log = LoggerFactory.getLogger(EfsStorageService.class);

    private final Path ticketsRoot;

    public EfsStorageService(AppProperties props) {
        this.ticketsRoot = Paths.get(props.efs().basePath(), "tickets").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(ticketsRoot);
            log.info("EFS listo. Tickets en: {}", ticketsRoot);
        } catch (IOException e) {
            throw new StorageException("No se pudo crear la carpeta de tickets en EFS: " + ticketsRoot, e);
        }
    }

    /** Ruta relativa dentro de la carpeta tickets (misma estructura que en S3). */
    public static String relativePath(String eventId, String userId, String ticketCode) {
        return eventId + "/" + userId + "/" + ticketCode + ".pdf";
    }

    public Path save(String relativePath, byte[] content) {
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.info("Ticket guardado en EFS: {}", target);
            return target;
        } catch (IOException e) {
            throw new StorageException("Error al escribir en EFS: " + target, e);
        }
    }

    public byte[] read(String relativePath) {
        Path target = resolve(relativePath);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new StorageException("No se encontro el archivo en EFS: " + target, e);
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public void delete(String relativePath) {
        Path target = resolve(relativePath);
        try {
            Files.deleteIfExists(target);
            log.info("Ticket eliminado de EFS: {}", target);
        } catch (IOException e) {
            throw new StorageException("Error al eliminar de EFS: " + target, e);
        }
    }

    public Path absolute(String relativePath) {
        return resolve(relativePath);
    }

    /** Lista todos los PDF que hay hoy en el EFS. */
    public List<EfsFileInfo> listFiles() {
        try (Stream<Path> files = Files.walk(ticketsRoot)) {
            return files.filter(Files::isRegularFile)
                    .sorted(Comparator.naturalOrder())
                    .map(p -> {
                        try {
                            return new EfsFileInfo(p.toString(), Files.size(p),
                                    Files.getLastModifiedTime(p).toInstant());
                        } catch (IOException e) {
                            return new EfsFileInfo(p.toString(), -1, null);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new StorageException("No se pudo listar el EFS", e);
        }
    }

    public String root() {
        return ticketsRoot.toString();
    }

    /** Evita que una ruta "../" se salga de la carpeta de tickets. */
    private Path resolve(String relativePath) {
        Path p = ticketsRoot.resolve(relativePath).normalize();
        if (!p.startsWith(ticketsRoot)) {
            throw new StorageException("Ruta invalida: " + relativePath);
        }
        return p;
    }
}
