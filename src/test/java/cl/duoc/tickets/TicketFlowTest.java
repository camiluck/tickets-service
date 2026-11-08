package cl.duoc.tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.tickets.service.ResourceNotFoundException;
import cl.duoc.tickets.service.S3StorageService;

/**
 * Prueba el flujo completo (generar, subir, modificar, descargar, estadisticas, eliminar)
 * con S3 simulado en memoria y el "EFS" en una carpeta local.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockBean
    S3StorageService s3;

    final Map<String, byte[]> fakeBucket = new ConcurrentHashMap<>();

    @BeforeEach
    void fakeS3() {
        fakeBucket.clear();
        doAnswer(i -> fakeBucket.put(i.getArgument(0), i.getArgument(1)))
                .when(s3).upload(anyString(), any(byte[].class), anyString());
        when(s3.download(anyString())).thenAnswer(i -> {
            byte[] b = fakeBucket.get(i.<String>getArgument(0));
            if (b == null) throw new ResourceNotFoundException("no existe");
            return b;
        });
        doAnswer(i -> fakeBucket.remove(i.<String>getArgument(0))).when(s3).delete(anyString());
    }

    @Test
    void flujoCompleto() throws Exception {
        long id = crear("evento123", "usuarioX", "PAGADO", 25000);
        crear("evento123", "usuarioY", "RESERVADO", 25000);

        String code = String.format("ticket%03d", id);
        String key = "evento123/usuarioX/" + code + ".pdf";
        assertThat(fakeBucket).containsKey(key);
        assertThat(new String(fakeBucket.get(key), 0, 5)).isEqualTo("%PDF-");

        // historial con filtro
        mvc.perform(get("/api/tickets").param("eventId", "evento123").param("status", "PAGADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // modificar: cambia de usuario -> se mueve en S3 y sube la version
        mvc.perform(put("/api/tickets/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"usuarioZ\",\"seat\":\"B-12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileVersion").value(2))
                .andExpect(jsonPath("$.s3Key").value("evento123/usuarioZ/" + code + ".pdf"));
        assertThat(fakeBucket).doesNotContainKey(key).containsKey("evento123/usuarioZ/" + code + ".pdf");

        // descargar un ticket desde S3
        mvc.perform(get("/api/tickets/" + id + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        // descargar ZIP por evento
        mvc.perform(get("/api/tickets/download").param("eventId", "evento123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"));

        // estadisticas
        String stats = mvc.perform(get("/api/stats/events/evento123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(2))
                .andExpect(jsonPath("$.vendidos").value(1))
                .andExpect(jsonPath("$.reservados").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(stats).get("ingresosVentas").decimalValue())
                .isEqualByComparingTo("25000");

        // eliminar
        mvc.perform(delete("/api/tickets/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tickets/" + id)).andExpect(status().isNotFound());
        assertThat(fakeBucket).doesNotContainKey("evento123/usuarioZ/" + code + ".pdf");
    }

    @Test
    void validaRutaSegura() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"../etc\",\"eventName\":\"x\",\"userId\":\"u\",\"userName\":\"u\",\"price\":1}"))
                .andExpect(status().isBadRequest());
    }

    private long crear(String evento, String usuario, String estado, int precio) throws Exception {
        String body = """
                {"eventId":"%s","eventName":"Concierto Rock 2026","eventDate":"2026-12-15",
                 "venue":"Movistar Arena","userId":"%s","userName":"Juan Perez",
                 "userEmail":"juan@mail.com","seat":"A-10","price":%d,"status":"%s"}
                """.formatted(evento, usuario, precio, estado);
        String resp = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadedToS3").value(true))
                .andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(resp);
        return n.get("id").asLong();
    }
}
