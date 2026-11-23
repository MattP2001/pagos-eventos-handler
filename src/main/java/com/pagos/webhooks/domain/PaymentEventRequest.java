package com.pagos.webhooks.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Payload que envía PayGate en cada webhook. */
public record PaymentEventRequest(
        @NotNull @JsonProperty("event_id") UUID eventId,
        @NotNull @JsonProperty("payment_id") UUID paymentId,
        @NotNull PaymentStatus status,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull OffsetDateTime timestamp) {
}
