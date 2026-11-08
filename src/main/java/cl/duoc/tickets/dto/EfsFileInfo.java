package cl.duoc.tickets.dto;

import java.time.Instant;

public record EfsFileInfo(String path, long sizeBytes, Instant lastModified) {
}
