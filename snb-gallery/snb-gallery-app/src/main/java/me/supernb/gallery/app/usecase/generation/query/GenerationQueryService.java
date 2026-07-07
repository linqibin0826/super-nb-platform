package me.supernb.gallery.app.usecase.generation.query;

import java.time.Duration;
import java.util.List;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.model.read.GenerationDetail;
import me.supernb.gallery.domain.model.read.GenerationSummary;
import me.supernb.gallery.domain.model.read.Image;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import org.springframework.stereotype.Service;

/// 生成历史只读查询用例:列表(缩略图现签,缺失回退 null)、详情(输出/参考图逐张现签)。
@Service
public class GenerationQueryService {

    /// presigned URL 有效期;过期后前端需重新拉列表/详情换新链接。
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(10);

    private final GenerationRepository repo;
    private final ImageStoragePort storage;

    /// 构造:注入生成仓储与对象存储端口。
    public GenerationQueryService(GenerationRepository repo, ImageStoragePort storage) {
        this.repo = repo;
        this.storage = storage;
    }

    /// 生成历史分页列表:按创建时刻倒序,时间相同按 id 倒序稳定排序。thumbUrl 现签,
    /// 键来自仓储投影(可能是真缩略图,也可能是仓储侧对存量单的首图回退),两者都缺时为 null。
    public Page<GenerationSummary> list(long userId, int page, int pageSize) {
        GenerationRepository.PageRows rows = repo.list(userId, page, pageSize);
        List<GenerationSummary> items = rows.rows().stream()
                .map(r -> new GenerationSummary(
                        String.valueOf(r.id()), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                        r.cost(), r.elapsedMs(), r.error(),
                        r.thumbKey() == null ? null : storage.presignGet(r.thumbKey(), PRESIGN_TTL)))
                .toList();
        return Page.of(items, rows.total(), page, pageSize);
    }

    /// 详情:输出图与参考图逐张现签 presigned URL;不存在或不归属本人 → 404。
    public GenerationDetail detail(long id, long userId) {
        GenerationRepository.DetailRow r = repo.detail(id, userId)
                .orElseThrow(() -> GalleryException.generationNotFound(String.valueOf(id)));
        List<Image> outputs = r.outputKeys().stream()
                .map(k -> new Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        List<Image> refs = r.refKeys().stream()
                .map(k -> new Image(storage.presignGet(k, PRESIGN_TTL), null, null))
                .toList();
        return new GenerationDetail(
                String.valueOf(r.id()), r.createdAt(), r.prompt(), r.size(), r.n(), r.quality(), r.status(),
                r.cost(), r.elapsedMs(), r.groupName(), r.keyId(), r.error(), outputs, refs);
    }
}
