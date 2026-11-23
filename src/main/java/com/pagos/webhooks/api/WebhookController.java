package com.pagos.webhooks.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagos.webhooks.domain.PaymentEventRequest;
import com.pagos.webhooks.service.WebhookResult;
import com.pagos.webhooks.service.WebhookService;
import com.pagos.webhooks.service.WebhookService.ConcurrentDuplicateException;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/paygate")
public class WebhookController {

    private final WebhookService service;
    private final ObjectMapper mapper;

    public WebhookController(WebhookService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Recibe un evento de PayGate. Responde 200 tanto si se aplicó como si era un
     * reintento: PayGate sólo necesita saber que lo recibimos.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@Valid @RequestBody PaymentEventRequest event)
            throws Exception {
        WebhookResult result;
        try {
            result = service.process(event, mapper.writeValueAsString(event));
        } catch (ConcurrentDuplicateException e) {
            result = WebhookResult.DUPLICATE;
        }
        return ResponseEntity.ok(Map.of("result", result.name()));
    }
}
