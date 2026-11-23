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

## Pendiente de confirmar

- Qué hacer cuando los eventos de un pago llegan desordenados.
