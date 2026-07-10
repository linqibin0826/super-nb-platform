package me.supernb.content.infra.adapter.persistence;

import me.supernb.content.domain.port.repository.ArticleRepository;
import me.supernb.content.infra.adapter.persistence.dao.ArticleJpaRepository;
import me.supernb.content.infra.adapter.persistence.entity.ArticleEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// [ArticleRepository] 实现：slug 幂等 upsert，一事务内先查后写。
///
/// 并发同 slug 首发极小概率撞 `UNIQUE(slug)`——撞约束整单重试一轮，第二轮 findBySlug 必命中转 update。
@Repository
public class ArticleRepositoryAdapter implements ArticleRepository {

    private final ArticleJpaRepository articles;
    private final TransactionTemplate txTemplate;

    /// 构造：注入文章 DAO，事务管理器内部包成 TransactionTemplate。
    public ArticleRepositoryAdapter(ArticleJpaRepository articles, PlatformTransactionManager txManager) {
        this.articles = articles;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// slug 命中则全量覆盖，否则雪花取号新建。
    @Override
    public UpsertOutcome upsert(ArticleData data) {
        try {
            return tryUpsert(data);
        } catch (DataIntegrityViolationException e) {
            return tryUpsert(data);
        }
    }

    /// 事务体：先查后写。
    private UpsertOutcome tryUpsert(ArticleData data) {
        return txTemplate.execute(status -> articles.findBySlug(data.slug())
                .map(e -> {
                    e.apply(data);
                    articles.save(e);
                    return new UpsertOutcome(e.getId(), false);
                })
                .orElseGet(() -> {
                    ArticleEntity e = articles.save(new ArticleEntity(data));
                    return new UpsertOutcome(e.getId(), true);
                }));
    }
}
