package cl.duoc.tickets.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.tickets.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEventId(String eventId);

    List<Ticket> findByUserId(String userId);

    List<Ticket> findByEventIdAndUserId(String eventId, String userId);
}
