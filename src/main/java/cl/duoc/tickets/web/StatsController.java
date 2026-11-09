package cl.duoc.tickets.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.tickets.dto.EventStatsResponse;
import cl.duoc.tickets.service.StatsService;

// Reportar estadisticas de ventas y reservas por evento
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/events")
    public List<EventStatsResponse> todos() {
        return stats.todosLosEventos();
    }

    @GetMapping("/events/{eventId}")
    public EventStatsResponse porEvento(@PathVariable String eventId) {
        return stats.porEvento(eventId);
    }
}
