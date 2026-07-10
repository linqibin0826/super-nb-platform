package me.supernb.content.app.usecase.category.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.content.app.usecase.category.dto.SyncResult;
import me.supernb.content.domain.port.repository.CategoryRepository;

import java.util.List;

/// 分类整表同步命令（发布管线在 upsert 文章前调用）。
public record SyncCategoriesCommand(List<CategoryRepository.CategoryData> categories)
        implements Command<SyncResult> {
}
