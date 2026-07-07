package me.supernb.gallery.app;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// studio 生成历史用例:创建(上传 R2 + 落 4 表,幂等)、列表/详情(现签 presigned)、删除(清 R2 + 删行)。
@Service
public class Generations {

    private static final int THUMB_EDGE = 256;
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(10);

    private final GenerationRepository repo;
    private final ImageStoragePort storage;
    private final ThumbnailPort thumbnails;

    public Generations(GenerationRepository repo, ImageStoragePort storage, ThumbnailPort thumbnails) {
        this.repo = repo;
        this.storage = storage;
        this.thumbnails = thumbnails;
    }

    public record Created(String id, Instant createdAt) {
    }

    public Created create(GalleryDto.CreateGenerationCommand cmd) {
        // 幂等:本人已有该 id 直接返回,不重复上传/写库
        Optional<Instant> existing = repo.findCreatedAt(cmd.id(), cmd.userId());
        if (existing.isPresent()) {
            return new Created(cmd.id(), existing.get());
        }

        // 1) 输出图逐张上传(png),留首图字节给缩略图
        List<GenerationRepository.OutputImage> outputs = new ArrayList<>();
        byte[] firstBytes = null;
        List<GalleryDto.ImageBytes> outImgs = cmd.outputImages() == null ? List.of() : cmd.outputImages();
        for (int idx = 0; idx < outImgs.size(); idx++) {
            byte[] data = outImgs.get(idx).data();
            if (idx == 0) {
                firstBytes = data;
            }
            String key = "gen/" + cmd.userId() + "/" + cmd.id() + "/" + idx + ".png";
            storage.put(key, data, "image/png");
            outputs.add(new GenerationRepository.OutputImage(idx, key, data.length));
        }

        // 1b) 首图缩略图(尽力而为:坏图/异常不阻断,留 null 由列表回退首图)
        String thumbKey = null;
        if (firstBytes != null) {
            try {
                byte[] thumb = thumbnails.toPng(firstBytes, THUMB_EDGE);
                String key = "gen/" + cmd.userId() + "/" + cmd.id() + "/thumb.png";
                storage.put(key, thumb, "image/png");
                thumbKey = key;
            } catch (RuntimeException e) {
                thumbKey = null;
            }
        }

        // 2) 参考图去重上传(key 由 sha 内容寻址)
        List<GenerationRepository.RefImage> refs = new ArrayList<>();
        List<GalleryDto.RefBytes> refImgs = cmd.refImages() == null ? List.of() : cmd.refImages();
        for (int idx = 0; idx < refImgs.size(); idx++) {
            byte[] data = refImgs.get(idx).data();
            String contentType = refImgs.get(idx).contentType();
            String sha = storage.sha256(data);
            String key = "ref/" + cmd.userId() + "/" + sha + "." + extFor(contentType);
            if (!repo.refExists(cmd.userId(), sha)) {
                storage.put(key, data, contentType == null ? "image/png" : contentType);
            }
            refs.add(new GenerationRepository.RefImage(idx, sha, key, data.length));
        }

        // 3) 落 4 表(一个事务)
        Instant createdAt = repo.save(new GenerationRepository.SaveGeneration(
                cmd.id(), cmd.userId(), cmd.prompt(), cmd.size(), cmd.n(), cmd.quality(), cmd.status(),
                cmd.cost(), cmd.elapsedMs(), cmd.groupName(), cmd.keyId(), cmd.error(), thumbKey, outputs, refs));
        return new Created(cmd.id(), createdAt);
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

    public void delete(String id, long userId) {
        List<String> keys = repo.deleteReturningObjectKeys(id, userId)
                .orElseThrow(() -> GalleryException.generationNotFound(id));
        for (String key : keys) {
            storage.delete(key);
        }
    }

    private static String extFor(String contentType) {
        if (contentType == null) {
            return "png";
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }
}
