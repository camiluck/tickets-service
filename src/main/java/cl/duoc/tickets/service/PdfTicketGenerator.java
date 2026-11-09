package cl.duoc.tickets.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import cl.duoc.tickets.model.Ticket;

/** Genera el PDF personalizado de cada ticket usando la libreria OpenPDF. */
@Component
public class PdfTicketGenerator {

    public byte[] generar(Ticket t) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            doc.add(new Paragraph(t.getEventName(), titulo));
            doc.add(new Paragraph("Ticket: " + t.getTicketCode()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Evento: " + t.getEventId()));
            doc.add(new Paragraph("Fecha: " + t.getEventDate()));
            doc.add(new Paragraph("Lugar: " + t.getVenue()));
            doc.add(new Paragraph("Asistente: " + t.getUserName() + " (" + t.getUserId() + ")"));
            doc.add(new Paragraph("Email: " + t.getUserEmail()));
            doc.add(new Paragraph("Asiento: " + t.getSeat()));
            doc.add(new Paragraph("Precio: $" + t.getPrice()));
            doc.add(new Paragraph("Estado: " + t.getStatus()));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Version del archivo: " + t.getFileVersion()));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new StorageException("Error al generar el PDF: " + e.getMessage());
        }
    }
}
