package cl.duoc.tickets.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.tickets.dto.CreateTicketRequest;
import cl.duoc.tickets.dto.TicketResponse;
import cl.duoc.tickets.dto.UpdateTicketRequest;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.model.TicketStatus;
import cl.duoc.tickets.service.TicketService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    // Generar ticket (PDF en EFS + subida automatica a S3)
    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody CreateTicketRequest req) {
        Ticket t = service.crear(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(t));
    }

    // Historial de tickets (filtros opcionales)
    @GetMapping
    public List<TicketResponse> listar(@RequestParam(required = false) String eventId,
                                       @RequestParam(required = false) String userId,
                                       @RequestParam(required = false) TicketStatus status) {
        return service.listar(eventId, userId, status).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse buscar(@PathVariable Long id) {
        return TicketResponse.from(service.buscar(id));
    }

    // Subir ticket a S3
    @PostMapping("/{id}/upload")
    public TicketResponse subir(@PathVariable Long id) {
        return TicketResponse.from(service.subir(id));
    }

    // Descargar un ticket desde S3
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Ticket t = service.buscar(id);
        byte[] archivo = service.descargar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + t.getTicketCode() + ".pdf")
                .body(archivo);
    }

    // Descargar tickets por usuario o evento (ZIP desde S3)
    @GetMapping("/download")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) String eventId,
                                               @RequestParam(required = false) String userId) {
        byte[] zip = service.descargarZip(eventId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.zip")
                .body(zip);
    }

    // Modificar detalles del ticket
    @PutMapping("/{id}")
    public TicketResponse actualizar(@PathVariable Long id, @RequestBody UpdateTicketRequest req) {
        return TicketResponse.from(service.actualizar(id, req));
    }

    // Eliminar ticket
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
