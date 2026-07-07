package me.supernb.gallery.app.usecase.generation.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.read.GenerationSummary;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.port.GenerationRepository;
import me.supernb.gallery.domain.port.ImageStoragePort;
import org.junit.jupiter.api.Test;

/// 生成历史查询用例:列表缩略图现签/无缩略图回退 null、详情 404。
class GenerationQueriesTest {

    private final GenerationRepository repo = mock(GenerationRepository.class);
    private final ImageStoragePort storage = mock(ImageStoragePort.class);
    private final GenerationQueries queries = new GenerationQueries(repo, storage);

    @Test
    void listPresignsThumbAndFallsBackToNull() {
        when(repo.list(7L, 1, 24)).thenReturn(new GenerationRepository.PageRows(List.of(
                new GenerationRepository.ListRow("g1", Instant.now(), "p", "1024x1024", 1, "medium", "done", 0.04, 1, null, "gen/7/g1/thumb.png"),
                new GenerationRepository.ListRow("g2", Instant.now(), "p", "1024x1024", 1, "medium", "error", null, 1, "boom", null)),
                2));
        when(storage.presignGet("gen/7/g1/thumb.png", java.time.Duration.ofMinutes(10))).thenReturn("https://signed/g1");

        Page<GenerationSummary> page = queries.list(7L, 1, 24);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items().get(0).thumbUrl()).isEqualTo("https://signed/g1");
        assertThat(page.items().get(1).thumbUrl()).isNull();
    }

    @Test
    void detailNotFoundThrows() {
        when(repo.detail("gx", 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> queries.detail("gx", 7L)).isInstanceOf(GalleryException.class);
    }
}
