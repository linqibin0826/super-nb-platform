package me.supernb.gallery.app.usecase.generation;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.gallery.app.usecase.generation.command.DeleteGenerationCommand;
import me.supernb.gallery.domain.exception.GalleryException;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import org.springframework.stereotype.Service;

/// 删除生成记录:先删行拿回其对象键,再逐个清 R2。不存在或不归属本人 → 404。
///
/// 拿回的对象键只覆盖这条记录自己的输出图与缩略图;参考图内容库按哈希去重、
/// 可能仍被该用户其他生成记录引用,不随这次删除一起清(`GenerationRepository.deleteReturningObjectKeys`
/// 的返回契约本就不包含它)。
@Service
public class DeleteGenerationHandler implements CommandHandler<DeleteGenerationCommand, Void> {

    private final GenerationRepository repo;
    private final ImageStoragePort storage;

    /// 构造:注入生成仓储与对象存储端口。
    public DeleteGenerationHandler(GenerationRepository repo, ImageStoragePort storage) {
        this.repo = repo;
        this.storage = storage;
    }

    /// 删行拿回对象键后逐个清 R2;不存在或不归属本人 → 404。
    @Override
    public Void handle(DeleteGenerationCommand command) {
        List<String> keys = repo.deleteReturningObjectKeys(command.id(), command.userId())
                .orElseThrow(() -> GalleryException.generationNotFound(String.valueOf(command.id())));
        for (String key : keys) {
            storage.delete(key);
        }
        return null;
    }
}
