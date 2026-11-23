package com.pagos.webhooks.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StateMachineTest {

    @Test
    void avanzaEnElOrdenNormal() {
        assertThat(StateMachine.advances(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED)).isTrue();
        assertThat(StateMachine.advances(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED)).isTrue();
        assertThat(StateMachine.advances(PaymentStatus.CAPTURED, PaymentStatus.REFUNDED)).isTrue();
    }

    @Test
    void unEventoRezagadoNoDegradaElEstado() {
        assertThat(StateMachine.advances(PaymentStatus.CAPTURED, PaymentStatus.AUTHORIZED)).isFalse();
        assertThat(StateMachine.advances(PaymentStatus.CAPTURED, PaymentStatus.PENDING)).isFalse();
    }

    @Test
    void unPagoFallidoNoSeRecupera() {
        assertThat(StateMachine.advances(PaymentStatus.FAILED, PaymentStatus.CAPTURED)).isFalse();
        assertThat(StateMachine.advances(PaymentStatus.FAILED, PaymentStatus.REFUNDED)).isFalse();
    }

    @Test
    void elMismoEstadoNoEsUnAvance() {
        assertThat(StateMachine.advances(PaymentStatus.CAPTURED, PaymentStatus.CAPTURED)).isFalse();
    }
}
