package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.sub2api.account.AccountAgeReadModel;
import org.junit.jupiter.api.Test;

/// 薄委托:固定 Asia/Shanghai 时区,把"今天减 N 天"换算交给调用方无需重复。
class AccountAnniversaryReadAdapterTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AccountAgeReadModel readModel = mock(AccountAgeReadModel.class);
    private final AccountAnniversaryReadAdapter adapter = new AccountAnniversaryReadAdapter(readModel);

    @Test
    void delegatesWithTodayMinusDaysAndShanghaiZone() {
        LocalDate today = LocalDate.now(ZONE);
        when(readModel.registeredOn(today.minusDays(100), ZONE)).thenReturn(List.of(1L, 2L));
        assertThat(adapter.registeredExactlyDaysAgo(100)).containsExactly(1L, 2L);
    }
}
