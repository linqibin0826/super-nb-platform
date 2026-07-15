package me.supernb.invoice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 手续费边界(站长拍板):<1000 不可开;[1000,3000) 收 5% HALF_UP 两位;≥3000 免。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class FeePolicyTest {

    @Test
    void minimumBoundary() {
        assertThat(FeePolicy.meetsMinimum(new BigDecimal("999.99"))).isFalse();
        assertThat(FeePolicy.meetsMinimum(new BigDecimal("1000"))).isTrue();
    }

    @Test
    void feeIsFivePercentHalfUpBelowFreeThreshold() {
        assertThat(FeePolicy.feeFor(new BigDecimal("1000"))).isEqualByComparingTo("50.00");
        assertThat(FeePolicy.feeFor(new BigDecimal("2999.99"))).isEqualByComparingTo("150.00"); // 149.9995 → HALF_UP
        assertThat(FeePolicy.feeFor(new BigDecimal("1234.51"))).isEqualByComparingTo("61.73");  // 61.7255 → 61.73
    }

    @Test
    void freeAtAndAboveThreshold() {
        assertThat(FeePolicy.feeFor(new BigDecimal("3000"))).isEqualByComparingTo("0.00");
        assertThat(FeePolicy.feeFor(new BigDecimal("9999"))).isEqualByComparingTo("0.00");
    }
}
