package me.supernb.gallery.app.usecase.generation;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.supernb.gallery.app.usecase.generation.command.CreateGenerationCommand;
import me.supernb.gallery.app.usecase.generation.command.ImageBytes;
import me.supernb.gallery.app.usecase.generation.command.RefBytes;
import me.supernb.gallery.app.usecase.generation.dto.Created;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.gallery.domain.port.thumbnail.ThumbnailPort;
import org.springframework.stereotype.Service;

/// 创建生成记录:输出图逐张上传 R2 + 首图缩略图(尽力而为)+ 参考图内容寻址去重 + 落 4 表(一个事务)。
///
/// 不做创建级幂等:id 在 `handle()` 内现取(`nextId()` 预分配),重复调用各生成一条新记录;
/// 唯一的去重发生在参考图这一层(按内容哈希,已存在则跳过物理上传,但仍会新增一条关联行)。
@Service
public class CreateGenerationHandler implements CommandHandler<CreateGenerationCommand, Created> {

    /// 列表缩略图长边上限(px);`ThumbnailPort` 保比例只缩不放。
    private static final int THUMB_EDGE = 256;

    private final GenerationRepository repo;
    private final ImageStoragePort storage;
    private final ThumbnailPort thumbnails;

    /// 构造:注入生成仓储、对象存储端口与缩略图端口。
    public CreateGenerationHandler(GenerationRepository repo, ImageStoragePort storage, ThumbnailPort thumbnails) {
        this.repo = repo;
        this.storage = storage;
        this.thumbnails = thumbnails;
    }

    /// 取号 → 逐张上传输出图 → 尽力生成首图缩略图 → 参考图去重上传 → 一个事务落 4 表,返回创建结果。
    @Override
    public Created handle(CreateGenerationCommand cmd) {
        // 身份 = 服务端预分配雪花(验收意见⑦):R2 键按它命名,必须先取号再上传/落库
        long id = repo.nextId();

        // 1) 输出图逐张上传(固定按 png 存,不转码/不探测实际格式),留首图字节给下面的缩略图用
        List<GenerationRepository.OutputImage> outputs = new ArrayList<>();
        byte[] firstBytes = null;
        List<ImageBytes> outImgs = cmd.outputImages() == null ? List.of() : cmd.outputImages();
        for (int idx = 0; idx < outImgs.size(); idx++) {
            byte[] data = outImgs.get(idx).data();
            if (idx == 0) {
                firstBytes = data;
            }
            String key = "gen/" + cmd.userId() + "/" + id + "/" + idx + ".png";
            storage.put(key, data, "image/png");
            outputs.add(new GenerationRepository.OutputImage(idx, key, data.length));
        }

        // 1b) 首图缩略图,尽力而为:坏图或异常不阻断创建,留 null 交由列表查询回退到首张输出图
        String thumbKey = null;
        if (firstBytes != null) {
            try {
                byte[] thumb = thumbnails.toPng(firstBytes, THUMB_EDGE);
                String key = "gen/" + cmd.userId() + "/" + id + "/thumb.png";
                storage.put(key, thumb, "image/png");
                thumbKey = key;
            } catch (RuntimeException e) {
                thumbKey = null;
            }
        }

        // 2) 参考图内容寻址去重:key 由 sha 算出,库里已有该哈希就跳过物理上传(仍记关联行)
        List<GenerationRepository.RefImage> refs = new ArrayList<>();
        List<RefBytes> refImgs = cmd.refImages() == null ? List.of() : cmd.refImages();
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

        // 3) 一个事务落 4 表
        Instant createdAt = repo.save(new GenerationRepository.SaveGeneration(
                id, cmd.userId(), cmd.prompt(), cmd.size(), cmd.n(), cmd.quality(), cmd.status(),
                cmd.cost(), cmd.elapsedMs(), cmd.groupName(), cmd.keyId(), cmd.error(), thumbKey, outputs, refs));
        return new Created(String.valueOf(id), createdAt);
    }

    /// MIME 类型 → R2 键扩展名;只认 jpeg/webp,null 或其余一律回落 png。
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
