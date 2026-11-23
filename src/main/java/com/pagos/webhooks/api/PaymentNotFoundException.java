package com.pagos.webhooks.api;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID paymentId) {
        super("No existe el pago " + paymentId);
    }
}
