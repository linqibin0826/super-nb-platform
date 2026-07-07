package me.supernb.gallery.app.usecase.generation.query;

import java.time.Duration;
import java.util.List;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.read.GenerationDetail;
import me.supernb.gallery.domain.model.read.GenerationSummary;
import me.supernb.gallery.domain.model.read.Image;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.port.GenerationRepository;
import me.supernb.gallery.domain.port.ImageStoragePort;
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

    public Page<GenerationSummary> list(long userId, int page, int pageSize) {
        GenerationRepository.PageRows rows = repo.list(userId, page, pageSize);
        List<GenerationSummary> items = rows.rows().stream()
                .map(r -> new GenerationSummary(
                        r.id(), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                        r.cost(), r.elapsedMs(), r.error(),
                        r.thumbKey() == null ? null : storage.presignGet(r.thumbKey(), PRESIGN_TTL)))
                .toList();
        return Page.of(items, rows.total(), page, pageSize);
    }

    public GenerationDetail detail(String id, long userId) {
        GenerationRepository.DetailRow r = repo.detail(id, userId)
                .orElseThrow(() -> GalleryException.generationNotFound(id));
        List<Image> outputs = r.outputKeys().stream()
                .map(k -> new Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        List<Image> refs = r.refKeys().stream()
                .map(k -> new Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        return new GenerationDetail(
                r.id(), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                r.cost(), r.elapsedMs(), r.groupName(), r.keyId(), r.error(), outputs, refs);
    }
}
