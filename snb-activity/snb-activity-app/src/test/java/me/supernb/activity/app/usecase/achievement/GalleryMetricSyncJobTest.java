package me.supernb.activity.app.usecase.achievement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.GalleryAchievementSignalPort;
import org.junit.jupiter.api.Test;

class GalleryMetricSyncJobTest {

    private final GalleryAchievementSignalPort signalPort = mock(GalleryAchievementSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);

    private GalleryMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new GalleryMetricSyncJob(signalPort, metricPort, settlementProperties);
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).syncDaily();
        verify(signalPort, never()).generationDoneCounts();
    }

    @Test
    void writesBothMetricsAsBatch() {
        when(signalPort.generationDoneCounts())
                .thenReturn(List.of(new GalleryAchievementSignalPort.UserCount(42L, 5L)));
        when(signalPort.likeAndFavoriteCounts())
                .thenReturn(List.of(new GalleryAchievementSignalPort.UserCount(42L, 20L)));
        job(true).syncDaily();
        verify(metricPort).upsertBatch("gallery_generate_done_count", Map.of(42L, 5.0));
        verify(metricPort).upsertBatch("gallery_like_fav_count", Map.of(42L, 20.0));
    }
}
