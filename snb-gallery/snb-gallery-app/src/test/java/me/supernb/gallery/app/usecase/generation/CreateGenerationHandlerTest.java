package me.supernb.gallery.app.usecase.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import me.supernb.gallery.app.usecase.generation.command.CreateGenerationCommand;
import me.supernb.gallery.app.usecase.generation.command.ImageBytes;
import me.supernb.gallery.app.usecase.generation.command.RefBytes;
import me.supernb.gallery.app.usecase.generation.dto.Created;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.gallery.domain.port.thumbnail.ThumbnailPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/// 创建生成记录 Handler:服务端 nextId 预分配即身份(R2 键按它命名)、
/// 缩略图尽力而为、参考图去重、落库参数与返回 id 字符串化。
class CreateGenerationHandlerTest {

    private final GenerationRepository repo = mock(GenerationRepository.class);
    private final ImageStoragePort storage = mock(ImageStoragePort.class);
    private final ThumbnailPort thumbnails = mock(ThumbnailPort.class);
    private final CreateGenerationHandler handler = new CreateGenerationHandler(repo, storage, thumbnails);

    private CreateGenerationCommand cmd(List<ImageBytes> outs, List<RefBytes> refs) {
        return new CreateGenerationCommand(
                7L, "a cat", "1024x1024", 1, "medium", "done",
                0.04, 1200, "grp", 9L, null, outs, refs);
    }

    @Test
    void createUploadsOutputThumbAndRefThenSavesUnderPreallocatedId() {
        when(repo.nextId()).thenReturn(42L);
        when(thumbnails.toPng(any(), eq(256))).thenReturn(new byte[] {9, 9});
        when(storage.sha256(any())).thenReturn("shaA");
        when(repo.refExists(7L, "shaA")).thenReturn(false);
        when(repo.save(any())).thenReturn(Instant.parse("2026-07-06T00:00:00Z"));

        Created created = handler.handle(cmd(
                List.of(new ImageBytes(new byte[] {1, 2})),
                List.of(new RefBytes(new byte[] {3}, "image/png"))));

        verify(storage).put(eq("gen/7/42/0.png"), any(), eq("image/png"));
        verify(storage).put(eq("gen/7/42/thumb.png"), any(), eq("image/png"));
        verify(storage).put(eq("ref/7/shaA.png"), any(), eq("image/png"));

        ArgumentCaptor<GenerationRepository.SaveGeneration> save =
                ArgumentCaptor.forClass(GenerationRepository.SaveGeneration.class);
        verify(repo).save(save.capture());
        assertThat(save.getValue().id()).isEqualTo(42L);
        assertThat(save.getValue().thumbKey()).isEqualTo("gen/7/42/thumb.png");
        assertThat(save.getValue().outputs()).hasSize(1);
        assertThat(save.getValue().refs()).hasSize(1);
        assertThat(created.id()).isEqualTo("42"); // 对外 id 一律字符串(雪花超 JS 安全整数)
    }

    @Test
    void thumbnailFailureLeavesNullThumbButKeepsOutput() {
        when(repo.nextId()).thenReturn(42L);
        when(thumbnails.toPng(any(), eq(256))).thenThrow(new RuntimeException("bad image"));
        when(repo.save(any())).thenReturn(Instant.now());

        handler.handle(cmd(List.of(new ImageBytes(new byte[] {1})), List.of()));

        verify(storage).put(eq("gen/7/42/0.png"), any(), any());
        ArgumentCaptor<GenerationRepository.SaveGeneration> save =
                ArgumentCaptor.forClass(GenerationRepository.SaveGeneration.class);
        verify(repo).save(save.capture());
        assertThat(save.getValue().thumbKey()).isNull();
    }

    @Test
    void refDedupSkipsUploadWhenAlreadyStored() {
        when(repo.nextId()).thenReturn(42L);
        when(storage.sha256(any())).thenReturn("shaDup");
        when(repo.refExists(7L, "shaDup")).thenReturn(true);
        when(repo.save(any())).thenReturn(Instant.now());

        handler.handle(cmd(List.of(), List.of(new RefBytes(new byte[] {3}, "image/png"))));

        verify(storage, never()).put(eq("ref/7/shaDup.png"), any(), any());
    }
}
