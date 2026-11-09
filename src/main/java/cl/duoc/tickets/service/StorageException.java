package cl.duoc.tickets.service;

/** Error al leer o escribir en EFS o S3. */
public class StorageException extends RuntimeException {
    public StorageException(String message) {
        super(message);
    }
}
