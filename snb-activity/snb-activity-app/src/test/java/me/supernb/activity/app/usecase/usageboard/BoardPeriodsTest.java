package me.supernb.activity.app.usecase.usageboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import org.junit.jupiter.api.Test;

class BoardPeriodsTest {

    // 2026-07-10 是周五;北京时间 2026-07-10 02:00 = UTC 2026-07-09 18:00(跨日陷阱用例)
    static final Instant NOW = Instant.parse("2026-07-09T18:00:00Z");

    @Test
    void dayStartsAtShanghaiMidnight() {
        assertThat(BoardPeriods.start(BoardPeriod.DAY, NOW)).isEqualTo(Instant.parse("2026-07-09T16:00:00Z"));
        assertThat(BoardPeriods.endsAt(BoardPeriod.DAY, NOW)).isEqualTo(Instant.parse("2026-07-10T16:00:00Z"));
    }

    @Test
    void weekStartsMonday() {
        assertThat(BoardPeriods.start(BoardPeriod.WEEK, NOW)).isEqualTo(Instant.parse("2026-07-05T16:00:00Z")); // 周一 07-06 00:00 CST
        assertThat(BoardPeriods.endsAt(BoardPeriod.WEEK, NOW)).isEqualTo(Instant.parse("2026-07-12T16:00:00Z"));
    }

    @Test
    void monthStartsFirstDay() {
        assertThat(BoardPeriods.start(BoardPeriod.MONTH, NOW)).isEqualTo(Instant.parse("2026-06-30T16:00:00Z"));
        assertThat(BoardPeriods.endsAt(BoardPeriod.MONTH, NOW)).isEqualTo(Instant.parse("2026-07-31T16:00:00Z"));
    }

    @Test
    void allIsEpochToNullEnd() {
        assertThat(BoardPeriods.start(BoardPeriod.ALL, NOW)).isEqualTo(Instant.EPOCH);
        assertThat(BoardPeriods.endsAt(BoardPeriod.ALL, NOW)).isNull();
    }
}
