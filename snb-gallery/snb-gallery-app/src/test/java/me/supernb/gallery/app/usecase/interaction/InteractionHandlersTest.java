package me.supernb.gallery.app.usecase.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptFavoriteCommand;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptLikeCommand;
import me.supernb.gallery.app.usecase.interaction.dto.FavResult;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.port.InteractionRepository;
import org.junit.jupiter.api.Test;

/// 点赞/收藏 Handler(mock 仓储端口)。
class InteractionHandlersTest {

    private final InteractionRepository interactionRepo = mock(InteractionRepository.class);

    @Test
    void likeReturnsCountAndFlag() {
        when(interactionRepo.toggleLike(5L, 7L, true)).thenReturn(OptionalInt.of(3));
        LikeResult r = new TogglePromptLikeHandler(interactionRepo)
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
        FavResult r = new TogglePromptFavoriteHandler(interactionRepo)
                .handle(new TogglePromptFavoriteCommand(5L, 7L, false));
        assertThat(r.favCount()).isZero();
        assertThat(r.favorited()).isFalse();
    }
}
