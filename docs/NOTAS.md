# Notas de la charla con el PO (Mario)

## Formato del webhook de PayGate

```json
{
  "event_id": "uuid",
  "payment_id": "uuid",
  "status": "PENDING|AUTHORIZED|CAPTURED|FAILED|REFUNDED",
  "amount": 1500.00,
  "currency": "ARS",
  "timestamp": "2023-10-25T14:30:00Z"
}
```

## Reintentos

- Si no reciben un 2xx en menos de 2 segundos, reintentan hasta 5 veces con backoff exponencial.
- En cada reintento el `event_id` es el mismo.
- Si el evento ya se procesó, se responde 200 sin volver a aplicar el cambio de estado.

## Orden de llegada

- PayGate no garantiza el orden: un evento con timestamp posterior puede llegar antes.
- Estados finales: `CAPTURED`, `FAILED` y `REFUNDED`.
- Un evento rezagado (por ejemplo un `AUTHORIZED` que llega cuando el pago ya está en
  `CAPTURED`) no degrada ni pisa el estado actual: el estado se queda en el más avanzado.
- El evento rezagado igual se guarda en el historial del pago.

## Alcance

- El servicio sólo recibe, persiste de forma consistente y expone la consulta.
- No tiene que avisarle a ningún otro sistema (no hay downstream para acreditar).
- Sin convenciones de ramas ni commits para este ejercicio. Lo importante: repo público,
  migraciones y un README que explique cómo levantarlo y cómo correr los tests.

## Dudas abiertas (para chequear con PayGate)

- ¿Puede llegar un `REFUNDED` sin un `CAPTURED` previo? Hoy se aplica.
- ¿Pueden variar los montos entre estados de un mismo pago? Hoy el monto consolidado es el del
  evento que avanza el estado.
