package cl.duoc.tickets.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo legible del ticket, ej: ticket001. Es el nombre del PDF. */
    @Column(unique = true)
    private String ticketCode;

    @Column(nullable = false)
    private String eventId;
    private String eventName;
    private LocalDate eventDate;
    private String venue;

    @Column(nullable = false)
    private String userId;
    private String userName;
    private String userEmail;

    private String seat;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    /** Ruta del PDF dentro del EFS. */
    private String efsPath;

    /** Llave del objeto en S3: {eventId}/{userId}/{ticketCode}.pdf */
    private String s3Key;
    private boolean uploadedToS3;
    private LocalDateTime uploadedAt;

    /** Se incrementa cada vez que se modifica el ticket y se regenera el PDF. */
    private int fileVersion;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) {
            status = TicketStatus.RESERVADO;
        }
        if (fileVersion == 0) {
            fileVersion = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- getters / setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public String getEfsPath() { return efsPath; }
    public void setEfsPath(String efsPath) { this.efsPath = efsPath; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public boolean isUploadedToS3() { return uploadedToS3; }
    public void setUploadedToS3(boolean uploadedToS3) { this.uploadedToS3 = uploadedToS3; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public int getFileVersion() { return fileVersion; }
    public void setFileVersion(int fileVersion) { this.fileVersion = fileVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
