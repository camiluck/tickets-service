package cl.duoc.tickets.dto;

import java.math.BigDecimal;

/**
 * Estadisticas de ventas y reservas de un evento.
 * reservados = tickets RESERVADO, vendidos = tickets PAGADO.
 */
public record EventStatsResponse(
        String eventId,
        String eventName,
        long totalTickets,
        long reservados,
        long vendidos,
        long cancelados,
        BigDecimal ingresosVentas,
        BigDecimal montoReservado) {
}
