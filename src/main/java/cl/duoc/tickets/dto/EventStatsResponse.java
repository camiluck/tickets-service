package cl.duoc.tickets.dto;

import java.math.BigDecimal;

/**
 * Estadisticas de ventas y reservas de un evento.
 *
 * @param totalTickets     tickets emitidos (incluye cancelados)
 * @param reservados       tickets en estado RESERVADO (reservas sin pagar)
 * @param vendidos         tickets en estado PAGADO (ventas)
 * @param cancelados       tickets en estado CANCELADO
 * @param ingresosVentas   suma del precio de los tickets PAGADO
 * @param montoReservado   suma del precio de los tickets RESERVADO (ingreso potencial)
 * @param usuariosUnicos   cantidad de usuarios distintos con tickets activos
 * @param subidosAS3       tickets cuyo PDF ya esta en S3
 */
public record EventStatsResponse(
        String eventId,
        String eventName,
        long totalTickets,
        long reservados,
        long vendidos,
        long cancelados,
        BigDecimal ingresosVentas,
        BigDecimal montoReservado,
        long usuariosUnicos,
        long subidosAS3) {
}
