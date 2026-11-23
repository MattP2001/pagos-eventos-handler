package com.pagos.webhooks.api;

import com.pagos.webhooks.repository.PaymentEventRepository;
import com.pagos.webhooks.repository.PaymentEventRepository.StoredEvent;
import com.pagos.webhooks.repository.PaymentRepository;
import com.pagos.webhooks.repository.PaymentRepository.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository payments;
    private final PaymentEventRepository events;

    public PaymentController(PaymentRepository payments, PaymentEventRepository events) {
        this.payments = payments;
        this.events = events;
    }

    /** Estado consolidado del pago y su historial cronológico (por timestamp del evento). */
    @GetMapping("/{paymentId}")
    public PaymentView get(@PathVariable UUID paymentId) {
        Payment payment = payments.find(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return new PaymentView(payment, events.findByPayment(paymentId));
    }

    public record PaymentView(Payment payment, List<StoredEvent> history) {
    }
}
