package me.supernb.activity.app.usecase.checkin.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/// TierInfo 扩展字段:group-id/校验期/成本随阈值+文案一起对外可读,供月度结算 job 消费。
class CheckinTierPropertiesTest {

    private final CheckinTierProperties props = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"),
            27L, 65L, 71L, new BigDecimal("0.9"), new BigDecimal("1.9"), new BigDecimal("4.4"));

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

    @Test
    void tierForAndLabelForStillWorkUnchanged() {
        assertThat(props.tierFor(new BigDecimal("36"))).contains("A");
        assertThat(props.labelFor("C")).isEqualTo("GPT-Pro 补给 · 7 天");
    }
}
