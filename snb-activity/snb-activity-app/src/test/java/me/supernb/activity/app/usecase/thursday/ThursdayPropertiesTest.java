package me.supernb.activity.app.usecase.thursday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import org.junit.jupiter.api.Test;

/// 场次表解析:空=休眠、正常解析、一场一组、格式错必须炸在启动期。
class ThursdayPropertiesTest {

    private ThursdayProperties props(String sessions) {
        return new ThursdayProperties(sessions, new BigDecimal("50"), 50, 1, "opening-fk");
    }

    @Test
    void blankSessionsMeansDormant() {
        assertThat(props("").groupIdFor(LocalDate.of(2026, 7, 30))).isNull();
        assertThat(props("   ").groupIdFor(LocalDate.of(2026, 7, 30))).isNull();
    }

    @Test
    void parsesSessionsAndMapsEachDateToItsOwnGroup() {
        ThursdayProperties p = props("2026-07-30=123, 2026-08-06=124 ,2026-08-13=125");
        assertThat(p.groupIdFor(LocalDate.of(2026, 7, 30))).isEqualTo(123L);
        assertThat(p.groupIdFor(LocalDate.of(2026, 8, 6))).isEqualTo(124L);
        assertThat(p.groupIdFor(LocalDate.of(2026, 8, 13))).isEqualTo(125L);
    }

    @Test
    void nonSessionDateIsNull() {
        assertThat(props("2026-07-30=123").groupIdFor(LocalDate.of(2026, 7, 31))).isNull();
    }

    /// 🚨 三场绝不能共用一个分组——sub2api 的 bulk-assign 幂等键是 (user_id, group_id),
    /// 共用会让第二场的回头客被静默判成"已领过"、拿不到第二张卡且看不出来。
    /// 本断言钉的是**理由**:任意两个场次的分组 id 必须互不相同。
    @Test
    void everySessionMustHaveItsOwnGroup() {
        ThursdayProperties p = props("2026-07-30=123,2026-08-06=124,2026-08-13=125");
        LocalDate[] dates = {LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 13)};
        assertThat(java.util.Arrays.stream(dates).map(p::groupIdFor).distinct().count())
                .as("三场疯四必须各用各的分组")
                .isEqualTo(dates.length);
    }

    @Test
    void malformedSessionsFailFastAtStartup() {
        assertThatThrownBy(() -> props("2026-07-30")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("=123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> props("2026-07-30=")).isInstanceOf(IllegalArgumentException.class);
    }
}
