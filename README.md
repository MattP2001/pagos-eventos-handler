# pagos-eventos-handler

Microservicio en Java (Spring Boot) que recibe los webhooks de eventos de pago de PayGate,
los procesa de forma idempotente y expone el estado consolidado y el historial de cada pago.

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/webhooks/paygate` | Recibe un evento de PayGate. Responde `200` con `APPLIED`, `RECORDED_ONLY` o `DUPLICATE` |
| `GET` | `/payments/{paymentId}` | Estado consolidado del pago e historial cronológico de eventos |

Ejemplo de webhook:

```json
{
  "event_id": "7c0e3b2a-5a2f-4f61-9a3e-0d6c1e2b9f10",
  "payment_id": "b1f7a8d4-2c9e-4c1b-8d1f-3e5a7b9c0d12",
  "status": "CAPTURED",
  "amount": 1500.00,
  "currency": "ARS",
  "timestamp": "2026-11-23T14:30:00Z"
}
```

## Cómo resuelve los problemas del escenario

**Reintentos (idempotencia).** PayGate reintenta hasta 5 veces con el mismo `event_id` si no
recibe un 2xx en 2 segundos. El `event_id` es la clave primaria de `payment_event`: un
reintento se detecta y se responde `200 DUPLICATE` sin volver a aplicar nada. Si dos
reintentos llegan a la vez, la PK hace que uno solo gane; el otro revierte su transacción y
también responde `DUPLICATE`.

**Eventos desordenados.** PayGate no garantiza el orden. Cada estado tiene un rango
(`PENDING` < `AUTHORIZED` < `CAPTURED`/`FAILED` < `REFUNDED`) y un evento sólo cambia el estado
si lo hace avanzar. Un evento rezagado se guarda en el historial con `applied = false` pero no
degrada el estado (`RECORDED_ONLY`).

**Concurrencia.** El procesamiento corre en una transacción y lee el pago con
`SELECT ... FOR UPDATE`, así dos eventos del mismo pago se aplican uno detrás del otro.

**Historial.** Se ordena por el `timestamp` del evento (cuándo ocurrió en PayGate), no por
cuándo llegó; `received_at` queda guardado para auditoría.

## Decisiones y supuestos

- **Monto del pago:** el PO no sabía si PayGate puede mandar montos distintos entre estados.
  Criterio: el monto consolidado es el del evento que hace avanzar el estado; cada evento
  conserva el suyo en el historial.
- **`REFUNDED` sin `CAPTURED` previo:** sin confirmar con el proveedor. Hoy se aplica porque
  `REFUNDED` es el estado más avanzado. Queda como duda abierta (ver `docs/NOTAS.md`).
- **`FAILED` es terminal:** un pago fallido no pasa a `CAPTURED` ni a `REFUNDED`.
- **Alcance:** no se notifica a ningún otro sistema; confirmado con el PO.

## Cómo correrlo

Requisitos: Java 21, Maven y Docker.

```bash
docker compose up -d          # Postgres en localhost:5432
mvn spring-boot:run           # aplica las migraciones de Flyway y levanta en :8080
```

Probarlo a mano:

```bash
curl -X POST localhost:8080/webhooks/paygate -H 'Content-Type: application/json' \
  -d '{"event_id":"7c0e3b2a-5a2f-4f61-9a3e-0d6c1e2b9f10","payment_id":"b1f7a8d4-2c9e-4c1b-8d1f-3e5a7b9c0d12","status":"CAPTURED","amount":1500.00,"currency":"ARS","timestamp":"2026-11-23T14:30:00Z"}'

curl localhost:8080/payments/b1f7a8d4-2c9e-4c1b-8d1f-3e5a7b9c0d12
```

## Tests

```bash
mvn test
```

Corren contra H2 en modo PostgreSQL, con las mismas migraciones. Cubren:

- un reintento del mismo evento no se aplica dos veces;
- un evento rezagado se guarda pero no pisa el estado;
- el orden normal hace avanzar el estado;
- payload inválido (`400`) y pago inexistente (`404`);
- las reglas de avance de estado, por separado (`StateMachineTest`).

## Estructura

```
src/main/java/com/pagos/webhooks/
├── api/          controladores y manejo de errores
├── domain/       payload del webhook, estados y reglas de avance
├── repository/   acceso a datos con JdbcTemplate (SQL explícito)
└── service/      procesamiento transaccional del webhook
src/main/resources/db/migration/   migraciones de Flyway
```
