package me.supernb.gallery.app.usecase.generation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import me.supernb.gallery.app.usecase.generation.command.DeleteGenerationCommand;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import org.junit.jupiter.api.Test;

/// 删除生成记录 Handler:删行后清 R2、不存在 404。
class DeleteGenerationHandlerTest {

    private final GenerationRepository repo = mock(GenerationRepository.class);
    private final ImageStoragePort storage = mock(ImageStoragePort.class);
    private final DeleteGenerationHandler handler = new DeleteGenerationHandler(repo, storage);

    @Test
    void deleteCleansReturnedKeys() {
        when(repo.deleteReturningObjectKeys(9L, 7L))
                .thenReturn(Optional.of(List.of("gen/7/9/0.png", "gen/7/9/thumb.png")));

        handler.handle(new DeleteGenerationCommand(9L, 7L));

        verify(storage).delete("gen/7/9/0.png");
        verify(storage).delete("gen/7/9/thumb.png");
    }

    @Test
    void deleteNotFoundThrows() {
        when(repo.deleteReturningObjectKeys(99L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new DeleteGenerationCommand(99L, 7L)))
                .isInstanceOf(GalleryException.class);
    }
}
