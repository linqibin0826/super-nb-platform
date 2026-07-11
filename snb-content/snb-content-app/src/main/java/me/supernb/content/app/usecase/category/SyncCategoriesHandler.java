package me.supernb.content.app.usecase.category;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.content.app.usecase.category.command.SyncCategoriesCommand;
import me.supernb.content.app.usecase.category.dto.SyncResult;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.springframework.stereotype.Service;

/// 分类整表同步用例：直接委托写端口（拒删守卫在仓储事务内），映射结果。
@Service
public class SyncCategoriesHandler implements CommandHandler<SyncCategoriesCommand, SyncResult> {

    private final CategoryRepository categoryRepository;

    /// 构造：注入分类写端口。
    public SyncCategoriesHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /// 处理整表同步命令。
    @Override
    public SyncResult handle(SyncCategoriesCommand cmd) {
        CategoryRepository.SyncOutcome outcome = categoryRepository.sync(cmd.categories());
        return new SyncResult(outcome.upserted(), outcome.deleted());
    }
}
