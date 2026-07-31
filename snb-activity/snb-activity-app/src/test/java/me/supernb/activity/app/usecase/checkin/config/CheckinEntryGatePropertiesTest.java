package me.supernb.activity.app.usecase.checkin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/// 准入闸配置:enabled 默认 false 保旧行为(不配 env 人人可签),金额字符串注入转
/// BigDecimal 照 CheckinBalanceProperties 惯例。
class CheckinEntryGatePropertiesTest {

    @Test
    void parsesWindowAndMin() {
        CheckinEntryGateProperties p = new CheckinEntryGateProperties(true, 30, "30");
        assertThat(p.enabled()).isTrue();
        assertThat(p.windowDays()).isEqualTo(30);
        assertThat(p.minCny()).isEqualByComparingTo("30");
    }

    @Test
    void disabledGateKeepsLegacyOpenDoor() {
        assertThat(new CheckinEntryGateProperties(false, 30, "30").enabled()).isFalse();
    }
}
