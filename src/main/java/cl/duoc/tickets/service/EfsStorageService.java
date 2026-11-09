package cl.duoc.tickets.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Almacenamiento temporal en Amazon EFS.
 *
 * El EFS esta montado en la EC2 en /mnt/efs y el contenedor lo ve en /app/efs
 * (docker run -v /mnt/efs:/app/efs ...).
 * Los PDF quedan en: /app/efs/tickets/{eventId}/{userId}/{ticketCode}.pdf
 */
@Service
public class EfsStorageService {

    @Value("${efs.path}")
    private String efsPath;

    /** Guarda el archivo en el EFS y devuelve la ruta completa. */
    public String guardar(String rutaRelativa, byte[] contenido) {
        try {
            Path destino = Paths.get(efsPath, "tickets", rutaRelativa);
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            return destino.toString();
        } catch (IOException e) {
            throw new StorageException("Error al guardar en EFS: " + e.getMessage());
        }
    }

    public byte[] leer(String rutaRelativa) {
        try {
            return Files.readAllBytes(Paths.get(efsPath, "tickets", rutaRelativa));
        } catch (IOException e) {
            throw new StorageException("Error al leer desde EFS: " + e.getMessage());
        }
    }

    public void eliminar(String rutaRelativa) {
        try {
            Files.deleteIfExists(Paths.get(efsPath, "tickets", rutaRelativa));
        } catch (IOException e) {
            throw new StorageException("Error al eliminar de EFS: " + e.getMessage());
        }
    }
}
