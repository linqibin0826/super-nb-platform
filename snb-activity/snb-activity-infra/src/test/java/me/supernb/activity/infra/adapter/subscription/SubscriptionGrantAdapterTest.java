package me.supernb.activity.infra.adapter.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import me.supernb.sub2api.admin.BulkAssignResult;
import me.supernb.sub2api.admin.Sub2apiAdminSubscriptionClient;
import org.junit.jupiter.api.Test;

/// 薄委托:结果原样转译为 activity 自己的领域类型,不泄漏 sub2api 包类型给上层。
class SubscriptionGrantAdapterTest {

    private final Sub2apiAdminSubscriptionClient client = mock(Sub2apiAdminSubscriptionClient.class);
    private final SubscriptionGrantAdapter adapter = new SubscriptionGrantAdapter(client);

    @Test
    void delegatesToClientAndMapsResult() {
        when(client.bulkAssign(List.of(42L), 65, 3, "n"))
                .thenReturn(new BulkAssignResult(Map.of(42L, "created"), List.of()));
        var outcome = adapter.bulkGrant(List.of(42L), 65, 3, "n");
        assertThat(outcome.statuses()).containsEntry(42L, "created");
        assertThat(outcome.errors()).isEmpty();
    }
}
