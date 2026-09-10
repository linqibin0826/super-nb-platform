package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.junit.jupiter.api.Test;

/// 排除分组名单解析 + 委托读模型:env 空 = 规则休眠(一次 SQL 都不发)。
class DrawExclusionAdapterTest {

    private final RechargeReadModel readModel = mock(RechargeReadModel.class);

    @Test
    void emptyConfigDisablesRuleWithoutQuerying() {
        DrawExclusionAdapter adapter = new DrawExclusionAdapter("", readModel);

        assertThat(adapter.isExcluded(7)).isFalse();
        verify(readModel, never()).inAnyAllowedGroup(anyLong(), anyCollection());
    }

    @Test
    void parsesCsvWithSpacesAndDelegates() {
        when(readModel.inAnyAllowedGroup(7, List.of(146L, 66L))).thenReturn(true);
        DrawExclusionAdapter adapter = new DrawExclusionAdapter(" 146, 66 ,", readModel);

        assertThat(adapter.isExcluded(7)).isTrue();
        assertThat(adapter.excludedGroupIds()).containsExactly(146L, 66L);
    }

    @Test
    void notExcludedWhenReadModelSaysNo() {
        when(readModel.inAnyAllowedGroup(anyLong(), anyCollection())).thenReturn(false);
        DrawExclusionAdapter adapter = new DrawExclusionAdapter("146", readModel);

        assertThat(adapter.isExcluded(7)).isFalse();
    }
}
