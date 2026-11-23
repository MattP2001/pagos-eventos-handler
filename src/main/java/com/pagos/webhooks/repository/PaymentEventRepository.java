package com.pagos.webhooks.repository;

import com.pagos.webhooks.domain.PaymentEventRequest;
import com.pagos.webhooks.domain.PaymentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentEventRepository {

    private final JdbcTemplate jdbc;

    public PaymentEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(UUID eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM payment_event WHERE event_id = ?", Integer.class, eventId);
        return count != null && count > 0;
    }

    public void insert(PaymentEventRequest event, boolean applied, String payload) {
        jdbc.update("""
                INSERT INTO payment_event
                    (event_id, payment_id, status, amount, currency, event_timestamp, applied, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.eventId(), event.paymentId(), event.status().name(), event.amount(),
                event.currency(), event.timestamp(), applied, payload);
    }

    public List<StoredEvent> findByPayment(UUID paymentId) {
        return jdbc.query("""
                SELECT event_id, status, amount, currency, event_timestamp, received_at, applied
                  FROM payment_event
                 WHERE payment_id = ?
                 ORDER BY event_timestamp, received_at
                """, this::map, paymentId);
    }

    private StoredEvent map(ResultSet rs, int row) throws SQLException {
        return new StoredEvent(
                rs.getObject("event_id", UUID.class),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getObject("event_timestamp", OffsetDateTime.class),
                rs.getObject("received_at", OffsetDateTime.class),
                rs.getBoolean("applied"));
    }

    public record StoredEvent(
            UUID eventId,
            PaymentStatus status,
            java.math.BigDecimal amount,
            String currency,
            OffsetDateTime eventTimestamp,
            OffsetDateTime receivedAt,
            boolean applied) {
    }
}
