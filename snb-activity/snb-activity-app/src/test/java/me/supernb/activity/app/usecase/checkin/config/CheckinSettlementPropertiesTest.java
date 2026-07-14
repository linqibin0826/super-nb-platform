package me.supernb.activity.app.usecase.checkin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CheckinSettlementPropertiesTest {

    @Test
    void defaultsAreSafeFalseAndSpecBudgetNumbers() {
        CheckinSettlementProperties props = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), false, false);
        assertThat(props.monthlyBudgetCap()).isEqualByComparingTo("250");
        assertThat(props.perUserMonthlyCap()).isEqualByComparingTo("10");
        assertThat(props.scanEnabled()).isFalse();
        assertThat(props.tierRewardEnabled()).isFalse();
    }
}
