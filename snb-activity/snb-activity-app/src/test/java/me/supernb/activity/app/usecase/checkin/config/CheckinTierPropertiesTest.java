package me.supernb.activity.app.usecase.checkin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/// TierInfo 扩展字段:group-id/校验期/成本随阈值+文案一起对外可读,供月度结算 job 消费。
class CheckinTierPropertiesTest {

    private final CheckinTierProperties props = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"),
            27L, 65L, 71L, new BigDecimal("0.9"), new BigDecimal("1.9"), new BigDecimal("4.4"), 20);

    @Test
    void tierInfoCarriesGroupIdValidityDaysAndCost() {
        var a = props.tiers().get(0);
        var b = props.tiers().get(1);
        var c = props.tiers().get(2);
        assertThat(a.groupId()).isEqualTo(27L);
        assertThat(a.validityDays()).isEqualTo(3);
        assertThat(a.costCny()).isEqualByComparingTo("0.9");
        assertThat(b.validityDays()).isEqualTo(3);
        assertThat(c.groupId()).isEqualTo(71L);
        assertThat(c.validityDays()).isEqualTo(7); // C 档 7 天,唯一不同档
        assertThat(c.costCny()).isEqualByComparingTo("4.4");
    }

    /// 2026-07-31 加时门槛由「满勤」放宽为「当月累计签满 N 天」,conditionText 是签到页
    /// 唯一上屏的档位条件文案,必须跟着改口径——否则页面判定按 20 天、说明还写「满勤」。
    @Test
    void conditionTextSpeaksCumulativeDaysNotFullMonth() {
        assertThat(props.tiers()).allSatisfy(t -> assertThat(t.conditionText()).doesNotContain("满勤"));
        assertThat(props.tiers().get(0).conditionText()).isEqualTo("当月签满 20 天 + 当月新增充值 ¥30 起");
        assertThat(props.tiers().get(2).conditionText()).isEqualTo("当月签满 20 天 + 当月新增充值 ¥500 起");
    }

    @Test
    void tierForAndLabelForStillWorkUnchanged() {
        assertThat(props.tierFor(new BigDecimal("36"))).contains("A");
        assertThat(props.labelFor("C")).isEqualTo("GPT-Pro 加时 · 7 天"); // 展示词随网吧 v2 叫「加时」
    }
}
