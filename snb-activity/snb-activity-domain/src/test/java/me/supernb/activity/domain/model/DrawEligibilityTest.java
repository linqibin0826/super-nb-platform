package me.supernb.activity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DrawEligibilityTest {

    @ParameterizedTest
    @CsvSource({
        "0,    0, 0",
        "99,   0, 0",
        "100,  0, 1",
        "250,  1, 1",
        "250,  2, 0",
        "250,  5, 0",
        "1000, 3, 7",
    })
    void computesRemainingDraws(String total, int used, int expected) {
        assertThat(DrawEligibility.remainingDraws(new BigDecimal(total), used)).isEqualTo(expected);
    }

    @Test
    void nullTotalIsZero() {
        assertThat(DrawEligibility.remainingDraws(null, 0)).isZero();
    }

    @Test
    void negativeBalanceGuard() {
        assertThat(DrawEligibility.remainingDraws(new BigDecimal("-50"), 0)).isZero();
    }
}
