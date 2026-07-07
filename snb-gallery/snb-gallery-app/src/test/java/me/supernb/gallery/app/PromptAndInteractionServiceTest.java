package me.supernb.gallery.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;
import me.supernb.gallery.domain.GalleryException;
import me.supernb.gallery.domain.SortMode;
import org.junit.jupiter.api.Test;

/// 提示词查询用例 + 点赞/收藏 Handler(mock 仓储端口)。
class PromptAndInteractionServiceTest {

    private final PromptRepository promptRepo = mock(PromptRepository.class);
    private final InteractionRepository interactionRepo = mock(InteractionRepository.class);

    @Test
    void detailNotFoundThrows() {
        when(promptRepo.detail(99L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> new PromptQueries(promptRepo).detail(99L)).isInstanceOf(GalleryException.class);
    }

    @Test
    void listParsesSortAndDelegates() {
        GalleryDto.Page<GalleryDto.PromptSummary> empty = GalleryDto.Page.of(java.util.List.of(), 0, 1, 24);
        when(promptRepo.list("style", "cat", SortMode.LIKES, 1, 24)).thenReturn(empty);
        assertThat(new PromptQueries(promptRepo).list("style", "cat", "likes", 1, 24)).isSameAs(empty);
    }

    @Test
    void likeReturnsCountAndFlag() {
        when(interactionRepo.toggleLike(5L, 7L, true)).thenReturn(OptionalInt.of(3));
        GalleryDto.LikeResult r = new TogglePromptLikeHandler(interactionRepo)
                .handle(new TogglePromptLikeCommand(5L, 7L, true));
        assertThat(r.likeCount()).isEqualTo(3);
        assertThat(r.liked()).isTrue();
    }

    @Test
    void likeOnMissingPromptThrows() {
        when(interactionRepo.toggleLike(5L, 7L, true)).thenReturn(OptionalInt.empty());
        assertThatThrownBy(() -> new TogglePromptLikeHandler(interactionRepo)
                .handle(new TogglePromptLikeCommand(5L, 7L, true)))
                .isInstanceOf(GalleryException.class);
    }

    @Test
    void favoriteReturnsCountAndFlag() {
        when(interactionRepo.toggleFavorite(5L, 7L, false)).thenReturn(OptionalInt.of(0));
        GalleryDto.FavResult r = new TogglePromptFavoriteHandler(interactionRepo)
                .handle(new TogglePromptFavoriteCommand(5L, 7L, false));
        assertThat(r.favCount()).isZero();
        assertThat(r.favorited()).isFalse();
    }
}
