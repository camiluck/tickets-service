package cl.duoc.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.tickets.model.TicketStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * Modificacion parcial: solo se cambian los campos que vienen con valor.
 * Si cambia userId o eventId, el archivo se mueve a la nueva carpeta en S3.
 */
public record UpdateTicketRequest(
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, numeros, - o _") String eventId,
        String eventName,
        LocalDate eventDate,
        String venue,
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, numeros, - o _") String userId,
        String userName,
        @Email String userEmail,
        String seat,
        @DecimalMin("0") BigDecimal price,
        TicketStatus status) {
}
