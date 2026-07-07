package me.supernb.gallery.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/// 创建生成记录 Handler:幂等短路、R2 上传/缩略图尽力而为/参考图去重、落库参数。
class CreateGenerationHandlerTest {

    private final GenerationRepository repo = mock(GenerationRepository.class);
    private final ImageStoragePort storage = mock(ImageStoragePort.class);
    private final ThumbnailPort thumbnails = mock(ThumbnailPort.class);
    private final CreateGenerationHandler handler = new CreateGenerationHandler(repo, storage, thumbnails);

    private CreateGenerationCommand cmd(List<GalleryDto.ImageBytes> outs, List<GalleryDto.RefBytes> refs) {
        return new CreateGenerationCommand(
                "task-1", 7L, "a cat", "1024x1024", 1, "medium", "done",
                0.04, 1200, "grp", 9L, null, outs, refs);
    }

    @Test
    void idempotentReturnsExistingWithoutUpload() {
        Instant when = Instant.parse("2026-07-06T00:00:00Z");
        when(repo.findCreatedAt("task-1", 7L)).thenReturn(Optional.of(when));

        GalleryDto.Created created =
                handler.handle(cmd(List.of(new GalleryDto.ImageBytes(new byte[] {1})), List.of()));

        assertThat(created.createdAt()).isEqualTo(when);
        verify(storage, never()).put(any(), any(), any());
        verify(repo, never()).save(any());
    }

    @Test
    void createUploadsOutputThumbAndRefThenSaves() {
        when(repo.findCreatedAt("task-1", 7L)).thenReturn(Optional.empty());
        when(thumbnails.toPng(any(), eq(256))).thenReturn(new byte[] {9, 9});
        when(storage.sha256(any())).thenReturn("shaA");
        when(repo.refExists(7L, "shaA")).thenReturn(false);
        when(repo.save(any())).thenReturn(Instant.parse("2026-07-06T00:00:00Z"));

        handler.handle(cmd(
                List.of(new GalleryDto.ImageBytes(new byte[] {1, 2})),
                List.of(new GalleryDto.RefBytes(new byte[] {3}, "image/png"))));

        verify(storage).put(eq("gen/7/task-1/0.png"), any(), eq("image/png"));
        verify(storage).put(eq("gen/7/task-1/thumb.png"), any(), eq("image/png"));
        verify(storage).put(eq("ref/7/shaA.png"), any(), eq("image/png"));

        ArgumentCaptor<GenerationRepository.SaveGeneration> save =
                ArgumentCaptor.forClass(GenerationRepository.SaveGeneration.class);
        verify(repo).save(save.capture());
        assertThat(save.getValue().thumbKey()).isEqualTo("gen/7/task-1/thumb.png");
        assertThat(save.getValue().outputs()).hasSize(1);
        assertThat(save.getValue().refs()).hasSize(1);
    }

    @Test
    void thumbnailFailureLeavesNullThumbButKeepsOutput() {
        when(repo.findCreatedAt("task-1", 7L)).thenReturn(Optional.empty());
        when(thumbnails.toPng(any(), eq(256))).thenThrow(new RuntimeException("bad image"));
        when(repo.save(any())).thenReturn(Instant.now());

        handler.handle(cmd(List.of(new GalleryDto.ImageBytes(new byte[] {1})), List.of()));

        verify(storage).put(eq("gen/7/task-1/0.png"), any(), any());
        ArgumentCaptor<GenerationRepository.SaveGeneration> save =
                ArgumentCaptor.forClass(GenerationRepository.SaveGeneration.class);
        verify(repo).save(save.capture());
        assertThat(save.getValue().thumbKey()).isNull();
    }

    @Test
    void refDedupSkipsUploadWhenAlreadyStored() {
        when(repo.findCreatedAt("task-1", 7L)).thenReturn(Optional.empty());
        when(storage.sha256(any())).thenReturn("shaDup");
        when(repo.refExists(7L, "shaDup")).thenReturn(true);
        when(repo.save(any())).thenReturn(Instant.now());

        handler.handle(cmd(List.of(), List.of(new GalleryDto.RefBytes(new byte[] {3}, "image/png"))));

        verify(storage, never()).put(eq("ref/7/shaDup.png"), any(), any());
    }
}
