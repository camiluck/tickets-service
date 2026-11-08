package cl.duoc.tickets.model;

/**
 * Estado del ticket.
 * RESERVADO = reserva sin pagar, PAGADO = venta concretada, CANCELADO = anulado.
 */
public enum TicketStatus {
    RESERVADO,
    PAGADO,
    CANCELADO
}
