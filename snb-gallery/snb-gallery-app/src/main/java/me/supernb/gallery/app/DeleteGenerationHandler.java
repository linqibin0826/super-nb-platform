package me.supernb.gallery.app;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.gallery.domain.GalleryException;
import org.springframework.stereotype.Service;

/// 删除生成记录:先删行(拿回对象键),再清 R2。不存在/非本人 → 404。
@Service
public class DeleteGenerationHandler implements CommandHandler<DeleteGenerationCommand, Void> {

    private final GenerationRepository repo;
    private final ImageStoragePort storage;

    public DeleteGenerationHandler(GenerationRepository repo, ImageStoragePort storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Override
    public Void handle(DeleteGenerationCommand command) {
        List<String> keys = repo.deleteReturningObjectKeys(command.id(), command.userId())
                .orElseThrow(() -> GalleryException.generationNotFound(command.id()));
        for (String key : keys) {
            storage.delete(key);
        }
        return null;
    }
}
