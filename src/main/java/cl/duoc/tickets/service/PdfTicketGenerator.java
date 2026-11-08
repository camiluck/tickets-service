package cl.duoc.tickets.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import cl.duoc.tickets.model.Ticket;

/** Genera el PDF personalizado de cada ticket. */
@Component
public class PdfTicketGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Color PRIMARY = new Color(32, 56, 100);

    public byte[] generate(Ticket t) {
        try {
            return build(t);
        } catch (Exception e) {
            throw new StorageException("Error al generar el PDF del ticket " + t.getTicketCode(), e);
        }
    }

    private byte[] build(Ticket t) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 36, 36, 30, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
        Font subtitle = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font value = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        Font small = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        Paragraph header = new Paragraph(nvl(t.getEventName(), t.getEventId()), title);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        Paragraph sub = new Paragraph("TICKET " + t.getTicketCode().toUpperCase() + "  |  " + t.getStatus(), subtitle);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        doc.add(sub);

        PdfPTable table = new PdfPTable(new float[] {1.2f, 2.8f});
        table.setWidthPercentage(100);
        row(table, "Evento (ID)", t.getEventId(), label, value);
        row(table, "Fecha", t.getEventDate() != null ? t.getEventDate().format(DATE) : "-", label, value);
        row(table, "Lugar", nvl(t.getVenue(), "-"), label, value);
        row(table, "Asistente", nvl(t.getUserName(), "-") + " (" + t.getUserId() + ")", label, value);
        row(table, "Email", nvl(t.getUserEmail(), "-"), label, value);
        row(table, "Asiento", nvl(t.getSeat(), "General"), label, value);
        row(table, "Precio", formatPrice(t), label, value);
        row(table, "Estado", t.getStatus().name(), label, value);
        doc.add(table);

        Paragraph footer = new Paragraph(
                "Version del archivo: " + t.getFileVersion()
                        + "   |   Generado: " + java.time.LocalDateTime.now().format(DATE_TIME)
                        + "   |   Ruta S3: " + t.getEventId() + "/" + t.getUserId() + "/" + t.getTicketCode() + ".pdf",
                small);
        footer.setSpacingBefore(12);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private static void row(PdfPTable table, String k, String v, Font kf, Font vf) {
        PdfPCell kc = new PdfPCell(new Phrase(k, kf));
        kc.setBackgroundColor(PRIMARY);
        kc.setPadding(6);
        kc.setBorderColor(Color.WHITE);
        PdfPCell vc = new PdfPCell(new Phrase(v, vf));
        vc.setPadding(6);
        vc.setBorderColor(new Color(220, 220, 220));
        table.addCell(kc);
        table.addCell(vc);
    }

    private static String formatPrice(Ticket t) {
        if (t.getPrice() == null) {
            return "-";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"));
        return nf.format(t.getPrice());
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
