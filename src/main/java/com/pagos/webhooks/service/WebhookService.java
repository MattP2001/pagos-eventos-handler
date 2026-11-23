package com.pagos.webhooks.service;

import com.pagos.webhooks.domain.PaymentEventRequest;
import com.pagos.webhooks.domain.StateMachine;
import com.pagos.webhooks.repository.PaymentEventRepository;
import com.pagos.webhooks.repository.PaymentRepository;
import com.pagos.webhooks.repository.PaymentRepository.Payment;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private final PaymentRepository payments;
    private final PaymentEventRepository events;

    public WebhookService(PaymentRepository payments, PaymentEventRepository events) {
        this.payments = payments;
        this.events = events;
    }

    /**
     * Procesa un webhook de PayGate en una sola transacción.
     *
     * 1. Si el event_id ya existe, es un reintento: no se aplica de nuevo.
     * 2. Se bloquea la fila del pago para serializar eventos concurrentes del mismo pago.
     * 3. El evento se guarda siempre en el historial; el estado sólo cambia si avanza.
     */
    @Transactional
    public WebhookResult process(PaymentEventRequest event, String rawPayload) {
        if (events.exists(event.eventId())) {
            return WebhookResult.DUPLICATE;
        }

        Optional<Payment> current = payments.findForUpdate(event.paymentId());
        boolean applied;
        if (current.isEmpty()) {
            payments.insert(new Payment(event.paymentId(), event.status(), event.amount(),
                    event.currency(), event.timestamp()));
            applied = true;
        } else if (StateMachine.advances(current.get().status(), event.status())) {
            payments.updateStatus(event.paymentId(), event.status(), event.timestamp());
            applied = true;
        } else {
            applied = false;
        }

        try {
            events.insert(event, applied, rawPayload);
        } catch (DuplicateKeyException e) {
            // Dos reintentos del mismo evento llegaron a la vez y el otro ganó la carrera.
            // La transacción se revierte entera: el cambio de estado lo aplicó el otro.
            throw new ConcurrentDuplicateException(e);
        }
        return applied ? WebhookResult.APPLIED : WebhookResult.RECORDED_ONLY;
    }

    public static class ConcurrentDuplicateException extends RuntimeException {
        ConcurrentDuplicateException(Throwable cause) {
            super(cause);
        }
    }
}
