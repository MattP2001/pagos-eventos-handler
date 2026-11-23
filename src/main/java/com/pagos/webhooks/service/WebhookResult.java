package com.pagos.webhooks.service;

/** Qué se hizo con un webhook recibido. */
public enum WebhookResult {
    /** Evento nuevo que cambió el estado del pago. */
    APPLIED,
    /** Evento nuevo que se guardó en el historial pero no cambió el estado (llegó tarde o desordenado). */
    RECORDED_ONLY,
    /** Reintento de un evento ya procesado: no se vuelve a aplicar. */
    DUPLICATE
}
