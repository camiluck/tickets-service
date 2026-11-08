package cl.duoc.tickets.dto;

import java.time.Instant;

public record S3ObjectInfo(String key, long sizeBytes, Instant lastModified) {
}
