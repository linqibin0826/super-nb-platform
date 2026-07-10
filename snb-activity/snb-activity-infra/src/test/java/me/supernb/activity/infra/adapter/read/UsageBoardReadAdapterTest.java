package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.sub2api.usageboard.UsageBoardReadModel;
import org.junit.jupiter.api.Test;

class UsageBoardReadAdapterTest {

    @Test
    void mapsReadModelRowsToDomainRows() {
        UsageBoardReadModel rm = mock(UsageBoardReadModel.class);
        when(rm.aggregate(any(), any())).thenReturn(List.of(
                new UsageBoardReadModel.UsageRow(10, "老王", "https://a", 500, 2, 4.0)));
        when(rm.eligible(10)).thenReturn(true);

        UsageBoardReadAdapter adapter = new UsageBoardReadAdapter(rm);
        List<UsageBoardRow> rows = adapter.aggregate(Instant.EPOCH, Instant.now());

        assertThat(rows).containsExactly(new UsageBoardRow(10, "老王", "https://a", 500, 2, 4.0));
        assertThat(adapter.eligible(10)).isTrue();
    }
}
