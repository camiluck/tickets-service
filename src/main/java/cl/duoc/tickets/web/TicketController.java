package cl.duoc.tickets.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    /** Generar ticket: crea el PDF en EFS y (si auto-upload) lo sube a S3. */
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(service.create(req)));
    }

    /** Historial de tickets generados, con filtros opcionales. */
    @GetMapping
    public List<TicketResponse> history(
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.search(eventId, userId, status, from, to).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    /** Subir (o volver a subir) el PDF del ticket desde EFS a S3. */
    @PostMapping("/{id}/upload")
    public TicketResponse upload(@PathVariable Long id) {
        return TicketResponse.from(service.upload(id));
    }

    /** Subir a S3 todos los tickets que estan solo en EFS. */
    @PostMapping("/upload-pending")
    public List<TicketResponse> uploadPending() {
        return service.uploadPending().stream().map(TicketResponse::from).toList();
    }

    /** Descargar el PDF de un ticket desde S3. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Ticket t = service.get(id);
        byte[] content = service.downloadFromS3(id);
        return file(content, MediaType.APPLICATION_PDF, t.getTicketCode() + ".pdf");
    }

    /** Descargar desde S3 un ZIP con los tickets de un evento y/o usuario. */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadZip(@RequestParam(required = false) String eventId,
                                              @RequestParam(required = false) String userId) {
        byte[] zip = service.downloadZipFromS3(eventId, userId);
        String name = "tickets"
                + (eventId != null ? "_" + eventId : "")
                + (userId != null ? "_" + userId : "") + ".zip";
        return file(zip, MediaType.parseMediaType("application/zip"), name);
    }

    /** Modificar detalles del ticket: regenera el PDF y lo reemplaza en S3. */
    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        return TicketResponse.from(service.update(id, req));
    }

    /** Eliminar ticket: borra el PDF de S3, de EFS y el registro. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<byte[]> file(byte[] content, MediaType type, String filename) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentLength(content.length)
                .body(content);
    }
}
