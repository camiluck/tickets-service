package cl.duoc.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.tickets.model.TicketStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para generar un ticket. eventId y userId se usan como carpetas en EFS y S3,
 * por eso solo aceptan letras, numeros, guion y guion bajo.
 */
public record CreateTicketRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, numeros, - o _") String eventId,
        @NotBlank String eventName,
        LocalDate eventDate,
        String venue,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,50}$", message = "solo letras, numeros, - o _") String userId,
        @NotBlank String userName,
        @Email String userEmail,
        String seat,
        @NotNull @DecimalMin("0") BigDecimal price,
        TicketStatus status) {
}
