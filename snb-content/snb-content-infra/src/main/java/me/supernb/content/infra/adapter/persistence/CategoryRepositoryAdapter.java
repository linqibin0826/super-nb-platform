package me.supernb.content.infra.adapter.persistence;

import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.port.repository.CategoryRepository;
import me.supernb.content.infra.adapter.persistence.dao.ArticleJpaRepository;
import me.supernb.content.infra.adapter.persistence.dao.ContentCategoryJpaRepository;
import me.supernb.content.infra.adapter.persistence.entity.ContentCategoryEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// [CategoryRepository] 实现：categories.yml 整表对齐，一事务内 upsert + 守卫式删除。
@Repository
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final ContentCategoryJpaRepository categories;
    private final ArticleJpaRepository articles;
    private final TransactionTemplate txTemplate;

    /// 构造：注入分类/文章 DAO（文章 DAO 只用于拒删守卫的引用计数）。
    public CategoryRepositoryAdapter(ContentCategoryJpaRepository categories, ArticleJpaRepository articles,
                                     PlatformTransactionManager txManager) {
        this.categories = categories;
        this.articles = articles;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// 分类是否存在。
    @Override
    public boolean exists(String slug) {
        return categories.existsById(slug);
    }

    /// 整表对齐；删除被引用分类时抛 [ContentException]（事务回滚，全量原样保留）。
    @Override
    public SyncOutcome sync(List<CategoryData> incoming) {
        return txTemplate.execute(status -> {
            Set<String> incomingSlugs = new HashSet<>();
            for (CategoryData c : incoming) {
                incomingSlugs.add(c.slug());
                categories.findById(c.slug())
                        .ifPresentOrElse(
                                e -> e.rename(c.name(), c.sortOrder()),
                                () -> categories.save(new ContentCategoryEntity(c.slug(), c.name(), c.sortOrder())));
            }
            int deleted = 0;
            for (ContentCategoryEntity existing : categories.findAll()) {
                if (incomingSlugs.contains(existing.getSlug())) {
                    continue;
                }
                long refs = articles.countByCategorySlug(existing.getSlug());
                if (refs > 0) {
                    throw ContentException.categoryInUse(existing.getSlug(), refs);
                }
                categories.delete(existing);
                deleted++;
            }
            return new SyncOutcome(incoming.size(), deleted);
        });
    }
}
