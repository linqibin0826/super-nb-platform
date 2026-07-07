package me.supernb.gallery.app;

import java.time.Duration;
import java.util.List;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// 生成历史只读查询用例:列表(缩略图现签,无缩略图回退 null)、详情(输出/参考图现签)。
@Service
public class GenerationQueries {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(10);

    private final GenerationRepository repo;
    private final ImageStoragePort storage;

    public GenerationQueries(GenerationRepository repo, ImageStoragePort storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public GalleryDto.Page<GalleryDto.GenerationSummary> list(long userId, int page, int pageSize) {
        GenerationRepository.PageRows rows = repo.list(userId, page, pageSize);
        List<GalleryDto.GenerationSummary> items = rows.rows().stream()
                .map(r -> new GalleryDto.GenerationSummary(
                        r.id(), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                        r.cost(), r.elapsedMs(), r.error(),
                        r.thumbKey() == null ? null : storage.presignGet(r.thumbKey(), PRESIGN_TTL)))
                .toList();
        return GalleryDto.Page.of(items, rows.total(), page, pageSize);
    }

    public GalleryDto.GenerationDetail detail(String id, long userId) {
        GenerationRepository.DetailRow r = repo.detail(id, userId)
                .orElseThrow(() -> GalleryException.generationNotFound(id));
        List<GalleryDto.Image> outputs = r.outputKeys().stream()
                .map(k -> new GalleryDto.Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        List<GalleryDto.Image> refs = r.refKeys().stream()
                .map(k -> new GalleryDto.Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        return new GalleryDto.GenerationDetail(
                r.id(), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                r.cost(), r.elapsedMs(), r.groupName(), r.keyId(), r.error(), outputs, refs);
    }
}
