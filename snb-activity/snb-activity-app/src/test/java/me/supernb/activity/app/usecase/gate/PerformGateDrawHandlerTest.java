package me.supernb.activity.app.usecase.gate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.app.usecase.gate.command.PerformGateDrawCommand;
import me.supernb.activity.app.usecase.gate.config.GateProperties;
import me.supernb.activity.domain.exception.GateAlreadyAttemptedTodayException;
import me.supernb.activity.domain.model.gate.GateDrawOutcome;
import me.supernb.activity.domain.port.gate.GatePort;
import me.supernb.activity.domain.port.read.GateRechargeReadPort;
import org.junit.jupiter.api.Test;

/// 抽签编排:休眠短路 / 门槛隐身 / RNG 判定进出 / 并发仲裁降级重读。
class PerformGateDrawHandlerTest {

    private final GatePort gatePort = mock(GatePort.class);
    private final GateRechargeReadPort recharge = mock(GateRechargeReadPort.class);

    private PerformGateDrawHandler handler(double winRate, String threshold, double roll) {
        return new PerformGateDrawHandler(
                new GateProperties(new BigDecimal(threshold), winRate), recharge, gatePort, () -> roll);
    }

    @Test
    void sleepModeShortCircuits() {
        GateDrawResult r = handler(0, "30", 0).handle(new PerformGateDrawCommand(7));
        assertThat(r.eligible()).isFalse();
        verifyNoInteractions(recharge, gatePort);
    }

    @Test
    void belowThresholdIsInvisible() {
        when(recharge.totalRecharged(7)).thenReturn(new BigDecimal("29.99"));
        GateDrawResult r = handler(0.02, "30", 0).handle(new PerformGateDrawCommand(7));
        assertThat(r.eligible()).isFalse();
        verifyNoInteractions(gatePort);
    }

    @Test
    void rollUnderRateAsksPortToWin() {
        when(recharge.totalRecharged(7)).thenReturn(new BigDecimal("30"));
        when(gatePort.drawFor(eq(7L), any(), eq(true)))
                .thenReturn(new GateDrawOutcome(true, new BigDecimal("6"), "T-1", Instant.EPOCH));
        GateDrawResult r = handler(0.02, "30", 0.019).handle(new PerformGateDrawCommand(7));
        assertThat(r.eligible()).isTrue();
        assertThat(r.win()).isTrue();
        assertThat(r.code()).isEqualTo("T-1");
    }

    @Test
    void rollOverRateDrawsAsLose() {
        when(recharge.totalRecharged(7)).thenReturn(new BigDecimal("100"));
        when(gatePort.drawFor(eq(7L), any(), eq(false))).thenReturn(GateDrawOutcome.lose());
        GateDrawResult r = handler(0.02, "30", 0.5).handle(new PerformGateDrawCommand(7));
        assertThat(r.eligible()).isTrue();
        assertThat(r.win()).isFalse();
    }

    @Test
    void uniqueRaceFallsBackToReplay() {
        when(recharge.totalRecharged(7)).thenReturn(new BigDecimal("30"));
        when(gatePort.drawFor(eq(7L), any(), eq(true))).thenThrow(new GateAlreadyAttemptedTodayException());
        when(gatePort.drawFor(eq(7L), any(), eq(false))).thenReturn(GateDrawOutcome.lose());
        GateDrawResult r = handler(1.0, "30", 0).handle(new PerformGateDrawCommand(7));
        assertThat(r.eligible()).isTrue();
        assertThat(r.win()).isFalse();
    }
}
