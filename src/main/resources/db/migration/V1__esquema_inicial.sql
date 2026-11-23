-- Estado consolidado de cada pago
CREATE TABLE payment (
    payment_id      UUID           PRIMARY KEY,
    status          VARCHAR(20)    NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL,
    last_event_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_status
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED'))
);

-- Todos los eventos recibidos de PayGate. event_id es la clave de idempotencia:
-- un reintento trae el mismo event_id y choca contra la PK.
CREATE TABLE payment_event (
    event_id        UUID           PRIMARY KEY,
    payment_id      UUID           NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    currency        CHAR(3)        NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied         BOOLEAN        NOT NULL,
    payload         TEXT           NOT NULL
);

CREATE INDEX idx_payment_event_payment_id ON payment_event (payment_id, event_timestamp);
