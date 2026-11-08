package cl.duoc.tickets.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import cl.duoc.tickets.config.AppProperties;
import cl.duoc.tickets.dto.CreateTicketRequest;
import cl.duoc.tickets.dto.UpdateTicketRequest;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.model.TicketStatus;
import cl.duoc.tickets.repository.TicketRepository;

/**
 * Logica principal: genera el PDF en EFS, lo sube a S3 y mantiene el registro del ticket.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String PDF = "application/pdf";

    private final TicketRepository repo;
    private final EfsStorageService efs;
    private final S3StorageService s3;
    private final PdfTicketGenerator pdf;
    private final boolean autoUpload;

    public TicketService(TicketRepository repo, EfsStorageService efs, S3StorageService s3,
                         PdfTicketGenerator pdf, AppProperties props) {
        this.repo = repo;
        this.efs = efs;
        this.s3 = s3;
        this.pdf = pdf;
        this.autoUpload = props.s3().autoUpload();
    }

    // ------------------------------------------------------------------ generar

    /** 1) Guarda el registro, 2) genera el PDF en EFS, 3) lo sube a S3 si auto-upload esta activo. */
    @Transactional
    public Ticket create(CreateTicketRequest req) {
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
        t = repo.save(t); // obtiene el id

        t.setTicketCode(String.format("ticket%03d", t.getId()));
        writeToEfs(t);

        if (autoUpload) {
            tryUpload(t);
        }
        return repo.save(t);
    }

    // ------------------------------------------------------------------ subir

    @Transactional
    public Ticket upload(Long id) {
        Ticket t = get(id);
        if (!efs.exists(relative(t))) {
            // el archivo temporal pudo haberse limpiado: se regenera desde los datos
            writeToEfs(t);
        }
        uploadToS3(t);
        return repo.save(t);
    }

    /** Sube todos los tickets que aun no estan en S3. */
    @Transactional
    public List<Ticket> uploadPending() {
        List<Ticket> uploaded = new ArrayList<>();
        for (Ticket t : repo.findByUploadedToS3False()) {
            if (!efs.exists(relative(t))) {
                writeToEfs(t);
            }
            uploadToS3(t);
            uploaded.add(repo.save(t));
        }
        return uploaded;
    }

    // ------------------------------------------------------------------ descargar

    public byte[] downloadFromS3(Long id) {
        Ticket t = get(id);
        if (!t.isUploadedToS3()) {
            throw new StorageException("El ticket " + t.getTicketCode()
                    + " aun no esta en S3. Usa POST /api/tickets/" + id + "/upload");
        }
        return s3.download(t.getS3Key());
    }

    /** ZIP con todos los tickets de un evento y/o usuario, descargados desde S3. */
    public byte[] downloadZipFromS3(String eventId, String userId) {
        if (!StringUtils.hasText(eventId) && !StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("Debes indicar eventId y/o userId");
        }
        List<Ticket> tickets = search(eventId, userId, null, null, null).stream()
                .filter(Ticket::isUploadedToS3)
                .toList();
        if (tickets.isEmpty()) {
            throw new ResourceNotFoundException("No hay tickets en S3 para eventId=" + eventId + ", userId=" + userId);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Ticket t : tickets) {
                zip.putNextEntry(new ZipEntry(t.getS3Key()));
                zip.write(s3.download(t.getS3Key()));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new StorageException("Error al armar el ZIP", e);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ modificar

    /**
     * Actualiza los datos, regenera el PDF (nueva version) en EFS y reemplaza el archivo en S3.
     * Si cambia el evento o el usuario, el archivo se mueve a la nueva carpeta y se borra el antiguo.
     */
    @Transactional
    public Ticket update(Long id, UpdateTicketRequest req) {
        Ticket t = get(id);
        String oldRelative = relative(t);
        boolean wasUploaded = t.isUploadedToS3();
        String oldS3Key = t.getS3Key();

        if (req.eventId() != null) t.setEventId(req.eventId());
        if (req.eventName() != null) t.setEventName(req.eventName());
        if (req.eventDate() != null) t.setEventDate(req.eventDate());
        if (req.venue() != null) t.setVenue(req.venue());
        if (req.userId() != null) t.setUserId(req.userId());
        if (req.userName() != null) t.setUserName(req.userName());
        if (req.userEmail() != null) t.setUserEmail(req.userEmail());
        if (req.seat() != null) t.setSeat(req.seat());
        if (req.price() != null) t.setPrice(req.price());
        if (req.status() != null) t.setStatus(req.status());

        t.setFileVersion(t.getFileVersion() + 1);
        String newRelative = relative(t);
        boolean moved = !Objects.equals(oldRelative, newRelative);

        writeToEfs(t);
        if (moved) {
            efs.delete(oldRelative);
        }

        if (wasUploaded || autoUpload) {
            uploadToS3(t); // sobrescribe el objeto (misma llave) o crea el de la nueva ruta
            if (wasUploaded && moved && oldS3Key != null) {
                s3.delete(oldS3Key);
            }
        }
        return repo.save(t);
    }

    // ------------------------------------------------------------------ eliminar

    @Transactional
    public void delete(Long id) {
        Ticket t = get(id);
        if (t.isUploadedToS3() && t.getS3Key() != null) {
            s3.delete(t.getS3Key());
        }
        efs.delete(relative(t));
        repo.delete(t);
    }

    // ------------------------------------------------------------------ consultas

    public Ticket get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket con id " + id));
    }

    /** Historial de tickets generados con filtros opcionales. */
    public List<Ticket> search(String eventId, String userId, TicketStatus status, LocalDate from, LocalDate to) {
        Specification<Ticket> spec = Specification.where(null);
        if (StringUtils.hasText(eventId)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("eventId"), eventId));
        }
        if (StringUtils.hasText(userId)) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("userId"), userId));
        }
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        if (from != null) {
            LocalDateTime f = from.atStartOfDay();
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.<LocalDateTime>get("createdAt"), f));
        }
        if (to != null) {
            LocalDateTime tt = to.plusDays(1).atStartOfDay();
            spec = spec.and((r, q, cb) -> cb.lessThan(r.<LocalDateTime>get("createdAt"), tt));
        }
        return repo.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // ------------------------------------------------------------------ helpers

    private static String relative(Ticket t) {
        return EfsStorageService.relativePath(t.getEventId(), t.getUserId(), t.getTicketCode());
    }

    private void writeToEfs(Ticket t) {
        String rel = relative(t);
        byte[] content = pdf.generate(t);
        efs.save(rel, content);
        t.setEfsPath(efs.absolute(rel).toString());
    }

    private void uploadToS3(Ticket t) {
        String key = relative(t); // evento123/usuarioX/ticket001.pdf
        s3.upload(key, efs.read(key), PDF);
        t.setS3Key(key);
        t.setUploadedToS3(true);
        t.setUploadedAt(LocalDateTime.now());
    }

    /** En la creacion, si S3 falla el ticket igual queda guardado en EFS (se puede subir despues). */
    private void tryUpload(Ticket t) {
        try {
            uploadToS3(t);
        } catch (StorageException e) {
            log.warn("No se pudo subir {} a S3 automaticamente: {}", t.getTicketCode(), e.getMessage());
            t.setUploadedToS3(false);
        }
    }
}
