package me.supernb.activity.infra.adapter.subscription;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import org.junit.jupiter.api.Test;

/// 余额发放适配器:零逻辑薄委托 admin 客户端的 add(),异常原样上抛交调用方转台账状态。
class BalanceGrantAdapterTest {

    private final Sub2apiAdminBalanceClient client = mock(Sub2apiAdminBalanceClient.class);
    private final BalanceGrantAdapter adapter = new BalanceGrantAdapter(client);

    @Test
    void delegatesToAdminClientAdd() {
        adapter.grant(42L, new BigDecimal("0.70"), "checkin-daily-2026-08-07");
        verify(client).add(42L, new BigDecimal("0.70"), "checkin-daily-2026-08-07");
    }
}
