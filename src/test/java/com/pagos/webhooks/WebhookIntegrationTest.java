package com.pagos.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM payment_event");
        jdbc.update("DELETE FROM payment");
    }

    private String evento(UUID eventId, UUID paymentId, String status, String timestamp) {
        return """
                {"event_id":"%s","payment_id":"%s","status":"%s",
                 "amount":1500.00,"currency":"ARS","timestamp":"%s"}
                """.formatted(eventId, paymentId, status, timestamp);
    }

    private void enviar(String body, String resultadoEsperado) throws Exception {
        mvc.perform(post("/webhooks/paygate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(resultadoEsperado));
    }

    @Test
    void unReintentoDelMismoEventoNoSeAplicaDosVeces() throws Exception {
        UUID pago = UUID.randomUUID();
        UUID evento = UUID.randomUUID();
        String body = evento(evento, pago, "CAPTURED", "2026-11-23T10:00:00Z");

        enviar(body, "APPLIED");
        enviar(body, "DUPLICATE");
        enviar(body, "DUPLICATE");

        Integer eventos = jdbc.queryForObject(
                "SELECT count(*) FROM payment_event WHERE payment_id = ?", Integer.class, pago);
        assertThat(eventos).isEqualTo(1);
    }

    @Test
    void unEventoRezagadoSeGuardaPeroNoPisaElEstado() throws Exception {
        UUID pago = UUID.randomUUID();
        enviar(evento(UUID.randomUUID(), pago, "CAPTURED", "2026-11-23T10:05:00Z"), "APPLIED");
        enviar(evento(UUID.randomUUID(), pago, "AUTHORIZED", "2026-11-23T10:01:00Z"), "RECORDED_ONLY");

        mvc.perform(get("/payments/" + pago))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.status").value("CAPTURED"))
                .andExpect(jsonPath("$.history.length()").value(2))
                // el historial se ordena por timestamp del evento, no por llegada
                .andExpect(jsonPath("$.history[0].status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.history[0].applied").value(false))
                .andExpect(jsonPath("$.history[1].status").value("CAPTURED"));
    }

    @Test
    void enOrdenNormalElEstadoAvanza() throws Exception {
        UUID pago = UUID.randomUUID();
        enviar(evento(UUID.randomUUID(), pago, "PENDING", "2026-11-23T10:00:00Z"), "APPLIED");
        enviar(evento(UUID.randomUUID(), pago, "AUTHORIZED", "2026-11-23T10:01:00Z"), "APPLIED");
        enviar(evento(UUID.randomUUID(), pago, "CAPTURED", "2026-11-23T10:02:00Z"), "APPLIED");

        mvc.perform(get("/payments/" + pago))
                .andExpect(jsonPath("$.payment.status").value("CAPTURED"))
                .andExpect(jsonPath("$.history.length()").value(3));
    }

    @Test
    void elMontoConsolidadoEsElDelEventoQueAvanza() throws Exception {
        UUID pago = UUID.randomUUID();
        enviar(evento(UUID.randomUUID(), pago, "AUTHORIZED", "2026-11-23T10:00:00Z"), "APPLIED");
        enviar("""
                {"event_id":"%s","payment_id":"%s","status":"CAPTURED",
                 "amount":1400.00,"currency":"ARS","timestamp":"2026-11-23T10:01:00Z"}
                """.formatted(UUID.randomUUID(), pago), "APPLIED");

        mvc.perform(get("/payments/" + pago))
                .andExpect(jsonPath("$.payment.amount").value(1400.00))
                .andExpect(jsonPath("$.history[0].amount").value(1500.00));
    }

    @Test
    void unPayloadInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/webhooks/paygate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_id\":\"no-es-uuid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarUnPagoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/payments/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
