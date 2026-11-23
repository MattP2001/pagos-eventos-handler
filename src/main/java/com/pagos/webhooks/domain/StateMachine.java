package com.pagos.webhooks.domain;

import java.util.Map;

/**
 * Reglas de avance de estado de un pago.
 *
 * Cada estado tiene un rango; un evento sólo cambia el estado si lo hace avanzar.
 * Un evento rezagado (de rango menor o igual) se guarda en el historial pero no pisa
 * el estado actual.
 */
public final class StateMachine {

    private static final Map<PaymentStatus, Integer> RANK = Map.of(
            PaymentStatus.PENDING, 0,
            PaymentStatus.AUTHORIZED, 1,
            PaymentStatus.CAPTURED, 2,
            PaymentStatus.FAILED, 2,
            PaymentStatus.REFUNDED, 3);

    private StateMachine() {
    }

    public static boolean isFinal(PaymentStatus status) {
        return status == PaymentStatus.CAPTURED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.REFUNDED;
    }

    /** true si pasar de {@code current} a {@code incoming} es un avance válido. */
    public static boolean advances(PaymentStatus current, PaymentStatus incoming) {
        if (current == incoming) {
            return false;
        }
        // Un pago fallido no se recupera ni se reembolsa.
        if (current == PaymentStatus.FAILED) {
            return false;
        }
        return RANK.get(incoming) > RANK.get(current);
    }
}
