package com.pagos.webhooks.repository;

import com.pagos.webhooks.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lee el pago bloqueando la fila (SELECT ... FOR UPDATE), para que dos webhooks
     * concurrentes del mismo pago se procesen uno detrás del otro.
     */
    public Optional<Payment> findForUpdate(UUID paymentId) {
        List<Payment> rows = jdbc.query("""
                SELECT payment_id, status, amount, currency, last_event_at
                  FROM payment
                 WHERE payment_id = ?
                   FOR UPDATE
                """,
                (rs, i) -> new Payment(
                        rs.getObject("payment_id", UUID.class),
                        PaymentStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency"),
                        rs.getObject("last_event_at", OffsetDateTime.class)),
                paymentId);
        return rows.stream().findFirst();
    }

    public Optional<Payment> find(UUID paymentId) {
        List<Payment> rows = jdbc.query("""
                SELECT payment_id, status, amount, currency, last_event_at
                  FROM payment
                 WHERE payment_id = ?
                """,
                (rs, i) -> new Payment(
                        rs.getObject("payment_id", UUID.class),
                        PaymentStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency"),
                        rs.getObject("last_event_at", OffsetDateTime.class)),
                paymentId);
        return rows.stream().findFirst();
    }

    /**
     * Crea el pago si no existe. Devuelve true si lo creó. Con ON CONFLICT DO NOTHING, dos
     * eventos simultáneos del mismo pago nuevo no fallan: uno lo crea y el otro espera y sigue.
     */
    public boolean insertIfAbsent(Payment p) {
        int rows = jdbc.update("""
                INSERT INTO payment (payment_id, status, amount, currency, last_event_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (payment_id) DO NOTHING
                """, p.paymentId(), p.status().name(), p.amount(), p.currency(), p.lastEventAt());
        return rows == 1;
    }

    public void applyEvent(UUID paymentId, PaymentStatus status, BigDecimal amount, String currency,
            OffsetDateTime lastEventAt) {
        jdbc.update("""
                UPDATE payment
                   SET status = ?, amount = ?, currency = ?, last_event_at = ?,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE payment_id = ?
                """, status.name(), amount, currency, lastEventAt, paymentId);
    }

    public record Payment(
            UUID paymentId,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            OffsetDateTime lastEventAt) {
    }
}
