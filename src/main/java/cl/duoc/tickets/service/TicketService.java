package cl.duoc.tickets.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import cl.duoc.tickets.dto.CreateTicketRequest;
import cl.duoc.tickets.dto.UpdateTicketRequest;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.model.TicketStatus;
import cl.duoc.tickets.repository.TicketRepository;

/**
 * Logica principal de los tickets:
 * genera el PDF, lo guarda en EFS (temporal) y lo sube a S3.
 */
@Service
public class TicketService {

    private final TicketRepository repo;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final PdfTicketGenerator pdf;

    public TicketService(TicketRepository repo, EfsStorageService efs,
                         S3StorageService s3, PdfTicketGenerator pdf) {
        this.repo = repo;
        this.efs = efs;
        this.s3 = s3;
        this.pdf = pdf;
    }

    /** Generar ticket: guarda el registro, crea el PDF en EFS y lo sube automaticamente a S3. */
    public Ticket crear(CreateTicketRequest req) {
        Ticket t = new Ticket();
        t.setEventId(req.eventId());
        t.setEventName(req.eventName());
        t.setEventDate(req.eventDate());
        t.setVenue(req.venue());
        t.setUserId(req.userId());
        t.setUserName(req.userName());
        t.setUserEmail(req.userEmail());
        t.setSeat(req.seat());
        t.setPrice(req.price());
        t.setStatus(req.status() != null ? req.status() : TicketStatus.RESERVADO);
        t.setFileVersion(1);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());

        t = repo.save(t); // al guardar se obtiene el id
        t.setTicketCode(String.format("ticket%03d", t.getId()));

        guardarEnEfsYSubirAS3(t);
        return repo.save(t);
    }

    /** Subir (o volver a subir) el PDF de un ticket a S3. */
    public Ticket subir(Long id) {
        Ticket t = buscar(id);
        guardarEnEfsYSubirAS3(t);
        return repo.save(t);
    }

    /** Modificar detalles: actualiza los datos, regenera el PDF y lo reemplaza en S3. */
    public Ticket actualizar(Long id, UpdateTicketRequest req) {
        Ticket t = buscar(id);
        if (req.eventName() != null) t.setEventName(req.eventName());
        if (req.eventDate() != null) t.setEventDate(req.eventDate());
        if (req.venue() != null) t.setVenue(req.venue());
        if (req.userName() != null) t.setUserName(req.userName());
        if (req.userEmail() != null) t.setUserEmail(req.userEmail());
        if (req.seat() != null) t.setSeat(req.seat());
        if (req.price() != null) t.setPrice(req.price());
        if (req.status() != null) t.setStatus(req.status());

        t.setFileVersion(t.getFileVersion() + 1);
        t.setUpdatedAt(LocalDateTime.now());

        guardarEnEfsYSubirAS3(t); // misma llave en S3, asi que se sobrescribe
        return repo.save(t);
    }

    /** Eliminar ticket: borra el PDF de S3, del EFS y el registro. */
    public void eliminar(Long id) {
        Ticket t = buscar(id);
        s3.eliminar(t.getS3Key());
        efs.eliminar(t.getS3Key());
        repo.delete(t);
    }

    /** Descargar el PDF de un ticket desde S3. */
    public byte[] descargar(Long id) {
        Ticket t = buscar(id);
        return s3.descargar(t.getS3Key());
    }

    /** Descargar desde S3 todos los tickets de un evento y/o usuario en un ZIP. */
    public byte[] descargarZip(String eventId, String userId) {
        List<Ticket> tickets = listar(eventId, userId, null);
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("No hay tickets para esos filtros");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(out);
            for (Ticket t : tickets) {
                zip.putNextEntry(new ZipEntry(t.getS3Key()));
                zip.write(s3.descargar(t.getS3Key()));
                zip.closeEntry();
            }
            zip.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Error al crear el ZIP: " + e.getMessage());
        }
    }

    /** Historial de tickets generados, con filtros opcionales por evento, usuario y estado. */
    public List<Ticket> listar(String eventId, String userId, TicketStatus status) {
        List<Ticket> tickets;
        if (eventId != null && userId != null) {
            tickets = repo.findByEventIdAndUserId(eventId, userId);
        } else if (eventId != null) {
            tickets = repo.findByEventId(eventId);
        } else if (userId != null) {
            tickets = repo.findByUserId(userId);
        } else {
            tickets = repo.findAll();
        }
        if (status != null) {
            tickets = tickets.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tickets;
    }

    public Ticket buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket con id " + id));
    }

    /** Genera el PDF, lo guarda en EFS y lo sube a S3 con la ruta evento/usuario/ticket.pdf */
    private void guardarEnEfsYSubirAS3(Ticket t) {
        String ruta = t.getEventId() + "/" + t.getUserId() + "/" + t.getTicketCode() + ".pdf";
        byte[] contenido = pdf.generar(t);

        String rutaEfs = efs.guardar(ruta, contenido);   // 1) almacenamiento temporal en EFS
        s3.subir(ruta, efs.leer(ruta));                  // 2) se sube a S3 desde el EFS

        t.setEfsPath(rutaEfs);
        t.setS3Key(ruta);
        t.setUploadedToS3(true);
    }
}
