package cl.duoc.tickets.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    public List<EventStatsResponse> allEvents() {
        Map<String, List<Ticket>> byEvent = repo.findAll().stream()
                .collect(Collectors.groupingBy(Ticket::getEventId, TreeMap::new, Collectors.toList()));
        return byEvent.entrySet().stream()
                .map(e -> build(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(EventStatsResponse::eventId))
                .toList();
    }

    public EventStatsResponse forEvent(String eventId) {
        List<Ticket> tickets = repo.findByEventId(eventId);
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("No hay tickets para el evento " + eventId);
        }
        return build(eventId, tickets);
    }

    private EventStatsResponse build(String eventId, List<Ticket> tickets) {
        String eventName = tickets.stream().map(Ticket::getEventName).filter(Objects::nonNull)
                .findFirst().orElse(eventId);
        long reservados = count(tickets, TicketStatus.RESERVADO);
        long vendidos = count(tickets, TicketStatus.PAGADO);
        long cancelados = count(tickets, TicketStatus.CANCELADO);
        long usuarios = tickets.stream().filter(t -> t.getStatus() != TicketStatus.CANCELADO)
                .map(Ticket::getUserId).distinct().count();
        long enS3 = tickets.stream().filter(Ticket::isUploadedToS3).count();

        return new EventStatsResponse(eventId, eventName, tickets.size(), reservados, vendidos, cancelados,
                sum(tickets, TicketStatus.PAGADO), sum(tickets, TicketStatus.RESERVADO), usuarios, enS3);
    }

    private static long count(List<Ticket> tickets, TicketStatus s) {
        return tickets.stream().filter(t -> t.getStatus() == s).count();
    }

    private static BigDecimal sum(List<Ticket> tickets, TicketStatus s) {
        return tickets.stream().filter(t -> t.getStatus() == s)
                .map(Ticket::getPrice).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
