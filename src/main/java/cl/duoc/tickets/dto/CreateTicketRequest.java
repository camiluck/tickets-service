package cl.duoc.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.tickets.model.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para generar un ticket.
 * eventId y userId se usan como nombres de carpeta en EFS y S3,
 * por eso solo se permiten letras, numeros, guion y guion bajo.
 */
public record CreateTicketRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]+$") String eventId,
        @NotBlank String eventName,
        LocalDate eventDate,
        String venue,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]+$") String userId,
        @NotBlank String userName,
        String userEmail,
        String seat,
        @NotNull BigDecimal price,
        TicketStatus status) {
}
