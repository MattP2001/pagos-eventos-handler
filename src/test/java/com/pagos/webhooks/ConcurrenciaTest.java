package com.pagos.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.pagos.webhooks.domain.PaymentEventRequest;
import com.pagos.webhooks.domain.PaymentStatus;
import com.pagos.webhooks.service.WebhookResult;
import com.pagos.webhooks.service.WebhookService;
import com.pagos.webhooks.service.WebhookService.ConcurrentDuplicateException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Simula los reintentos de PayGate llegando en paralelo con el mismo event_id. */
@SpringBootTest
class ConcurrenciaTest {

    @Autowired
    WebhookService service;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void reintentosEnParaleloSeAplicanUnaSolaVez() throws Exception {
        UUID pago = UUID.randomUUID();
        PaymentEventRequest evento = new PaymentEventRequest(UUID.randomUUID(), pago,
                PaymentStatus.CAPTURED, new BigDecimal("1500.00"), "ARS",
                OffsetDateTime.parse("2026-11-23T10:00:00Z"));

        int reintentos = 5;
        ExecutorService pool = Executors.newFixedThreadPool(reintentos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<WebhookResult>> resultados = new ArrayList<>();
        for (int i = 0; i < reintentos; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    return service.process(evento, "{}");
                } catch (ConcurrentDuplicateException e) {
                    return WebhookResult.DUPLICATE;
                }
            }));
        }
        largada.countDown();

        long aplicados = 0;
        for (Future<WebhookResult> r : resultados) {
            if (r.get() == WebhookResult.APPLIED) {
                aplicados++;
            }
        }
        pool.shutdown();

        assertThat(aplicados).isEqualTo(1);
        Integer eventos = jdbc.queryForObject(
                "SELECT count(*) FROM payment_event WHERE event_id = ?", Integer.class, evento.eventId());
        assertThat(eventos).isEqualTo(1);
    }
}
