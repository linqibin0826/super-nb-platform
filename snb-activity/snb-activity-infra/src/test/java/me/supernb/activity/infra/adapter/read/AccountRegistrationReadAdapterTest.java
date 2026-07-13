package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.junit.jupiter.api.Test;

/// 薄委托:直接转发既有 RaffleGateReadModel.registeredAts(签到账龄门槛复用 raffle 账龄门槛
/// 已用的同一只读能力,不新增 sub2api ACL 面)。
class AccountRegistrationReadAdapterTest {

    private final RaffleGateReadModel readModel = mock(RaffleGateReadModel.class);
    private final AccountRegistrationReadAdapter adapter = new AccountRegistrationReadAdapter(readModel);

    @Test
    void presentWhenUpstreamReturnsRegisteredAt() {
        Instant at = Instant.parse("2026-06-01T00:00:00Z");
        when(readModel.registeredAts(List.of(42L))).thenReturn(Map.of(42L, at));
        assertThat(adapter.registeredAt(42)).contains(at);
    }

    @Test
    void emptyWhenUpstreamMapLacksUser() {
        when(readModel.registeredAts(List.of(7L))).thenReturn(Map.of());
        assertThat(adapter.registeredAt(7)).isEmpty();
    }
}
