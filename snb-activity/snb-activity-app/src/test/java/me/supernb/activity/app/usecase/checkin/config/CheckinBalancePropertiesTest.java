package me.supernb.activity.app.usecase.checkin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/// 返网费配置:金额一律字符串注入转 BigDecimal,避免 @Value 直绑 double 丢精度
/// ——这几个数直接决定给用户发多少钱。
class CheckinBalancePropertiesTest {

    @Test
    void parsesDecimalsExactly() {
        CheckinBalanceProperties p = new CheckinBalanceProperties(true, "0.1", "30", "3000", 1);
        assertThat(p.enabled()).isTrue();
        assertThat(p.perDayCny()).isEqualByComparingTo("0.1");
        assertThat(p.thresholdCny()).isEqualByComparingTo("30");
        assertThat(p.monthlyCapCny()).isEqualByComparingTo("3000");
        assertThat(p.stepDays()).isEqualTo(1);
    }

    @Test
    void stepDaysCarriesPairPricing() {
        CheckinBalanceProperties p = new CheckinBalanceProperties(true, "0.1", "30", "3000", 2);
        assertThat(p.stepDays()).isEqualTo(2);
    }

    @Test
    void disabledSwitchIsRespected() {
        CheckinBalanceProperties p = new CheckinBalanceProperties(false, "0.1", "30", "3000", 1);
        assertThat(p.enabled()).isFalse();
    }
}
