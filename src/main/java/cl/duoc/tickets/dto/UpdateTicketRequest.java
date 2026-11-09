package cl.duoc.tickets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.duoc.tickets.model.TicketStatus;

/**
 * Datos que se pueden modificar de un ticket.
 * Solo se cambian los campos que vienen en el JSON (los demas quedan igual).
 */
public record UpdateTicketRequest(
        String eventName,
        LocalDate eventDate,
        String venue,
        String userName,
        String userEmail,
        String seat,
        BigDecimal price,
        TicketStatus status) {
}
