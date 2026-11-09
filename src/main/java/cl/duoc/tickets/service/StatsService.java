package cl.duoc.tickets.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import cl.duoc.tickets.dto.EventStatsResponse;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.model.TicketStatus;
import cl.duoc.tickets.repository.TicketRepository;

/** Estadisticas de ventas y reservas por evento. */
@Service
public class StatsService {

    private final TicketRepository repo;

    public StatsService(TicketRepository repo) {
        this.repo = repo;
    }

    /** Estadisticas de todos los eventos. */
    public List<EventStatsResponse> todosLosEventos() {
        Set<String> eventos = new LinkedHashSet<>();
        for (Ticket t : repo.findAll()) {
            eventos.add(t.getEventId());
        }
        List<EventStatsResponse> resultado = new ArrayList<>();
        for (String eventId : eventos) {
            resultado.add(porEvento(eventId));
        }
        return resultado;
    }

    /** Estadisticas de un evento. */
    public EventStatsResponse porEvento(String eventId) {
        List<Ticket> tickets = repo.findByEventId(eventId);
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("No hay tickets para el evento " + eventId);
        }

        long reservados = 0, vendidos = 0, cancelados = 0;
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal montoReservado = BigDecimal.ZERO;

        for (Ticket t : tickets) {
            if (t.getStatus() == TicketStatus.RESERVADO) {
                reservados++;
                montoReservado = montoReservado.add(t.getPrice());
            } else if (t.getStatus() == TicketStatus.PAGADO) {
                vendidos++;
                ingresos = ingresos.add(t.getPrice());
            } else if (t.getStatus() == TicketStatus.CANCELADO) {
                cancelados++;
            }
        }

        return new EventStatsResponse(eventId, tickets.get(0).getEventName(), tickets.size(),
                reservados, vendidos, cancelados, ingresos, montoReservado);
    }
}
