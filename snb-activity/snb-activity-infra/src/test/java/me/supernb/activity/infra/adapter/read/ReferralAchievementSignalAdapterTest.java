package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import me.supernb.sub2api.referral.ReferralReadModel;
import org.junit.jupiter.api.Test;

class ReferralAchievementSignalAdapterTest {

    private final ReferralReadModel readModel = mock(ReferralReadModel.class);
    private final ReferralAchievementSignalAdapter adapter = new ReferralAchievementSignalAdapter(readModel);

    @Test
    void delegatesToReadModel() {
        when(readModel.allInviteeIdsByInviter()).thenReturn(Map.of(1L, List.of(2L, 3L)));
        assertThat(adapter.allInviteeIdsByInviter()).containsEntry(1L, List.of(2L, 3L));
    }
}
