package me.supernb.gallery.app.usecase.prompt.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.PromptRepository;
import org.junit.jupiter.api.Test;

/// 提示词查询用例(mock 仓储端口)。
class PromptQueriesTest {

    private final PromptRepository promptRepo = mock(PromptRepository.class);

    @Test
    void detailNotFoundThrows() {
        when(promptRepo.detail(99L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> new PromptQueries(promptRepo).detail(99L)).isInstanceOf(GalleryException.class);
    }

    @Test
    void listParsesSortAndDelegates() {
        Page<PromptSummary> empty = Page.of(java.util.List.of(), 0, 1, 24);
        when(promptRepo.list("style", "cat", SortMode.LIKES, 1, 24)).thenReturn(empty);
        assertThat(new PromptQueries(promptRepo).list("style", "cat", "likes", 1, 24)).isSameAs(empty);
    }
}
