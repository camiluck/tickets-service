package cl.duoc.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.model.TicketStatus;

public record TicketResponse(
        Long id,
        String ticketCode,
        String eventId,
        String eventName,
        LocalDate eventDate,
        String venue,
        String userId,
        String userName,
        String userEmail,
        String seat,
        BigDecimal price,
        TicketStatus status,
        String efsPath,
        String s3Key,
        boolean uploadedToS3,
        LocalDateTime uploadedAt,
        int fileVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getTicketCode(), t.getEventId(), t.getEventName(),
                t.getEventDate(), t.getVenue(), t.getUserId(), t.getUserName(), t.getUserEmail(),
                t.getSeat(), t.getPrice(), t.getStatus(), t.getEfsPath(), t.getS3Key(),
                t.isUploadedToS3(), t.getUploadedAt(), t.getFileVersion(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
