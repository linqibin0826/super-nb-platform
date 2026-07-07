package me.supernb.gallery.infra.adapter.read;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryNode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.read.PromptReadPort;
import me.supernb.gallery.infra.adapter.persistence.dao.CategoryJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/// [PromptReadPort] 实现:gallery 库只读查询(列表分页、详情、三轴类目树)。
///
/// 列表的过滤/排序组合走动态 HQL 现场拼接:拼接片段全为常量,入参一律走 `:param` 绑定,不留注入面。
@Repository
public class PromptReadAdapter implements PromptReadPort {

    private final PromptJpaRepository prompts;
    private final CategoryJpaRepository categories;
    private final EntityManager em;

    /// 构造:注入提示词/类目 DAO,以及拼动态 HQL 用的 EntityManager。
    public PromptReadAdapter(PromptJpaRepository prompts, CategoryJpaRepository categories, EntityManager em) {
        this.prompts = prompts;
        this.categories = categories;
        this.em = em;
    }

    /// 已发布提示词分页(`page` 从 1 起);`categorySlug`/`q` 留空则不参与对应过滤维度,`q` 按标题/描述
    /// 关键字大小写不敏感匹配,无匹配 → 空页而非异常。
    @Override
    public Page<PromptSummary> list(
            String categorySlug, String q, SortMode sort, int page, int pageSize) {
        boolean byCategory = categorySlug != null && !categorySlug.isBlank();
        boolean byQuery = q != null && !q.isBlank();
        String from = "FROM PromptEntity p" + (byCategory ? " JOIN p.category c" : "");
        StringBuilder where = new StringBuilder(" WHERE p.status = 'published'");
        if (byCategory) {
            where.append(" AND c.slug = :cat");
        }
        if (byQuery) {
            where.append(" AND (LOWER(p.title) LIKE :q OR LOWER(p.description) LIKE :q)");
        }

        TypedQuery<Long> count = em.createQuery("SELECT COUNT(p) " + from + where, Long.class);
        TypedQuery<PromptEntity> rows = em.createQuery(
                "SELECT p " + from + where + " ORDER BY " + orderHql(sort), PromptEntity.class);
        if (byCategory) {
            count.setParameter("cat", categorySlug);
            rows.setParameter("cat", categorySlug);
        }
        if (byQuery) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            count.setParameter("q", like);
            rows.setParameter("q", like);
        }

        long total = count.getSingleResult();
        List<PromptSummary> items = rows
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList().stream()
                .map(PromptMapper::toSummary)
                .toList();
        return Page.of(items, total, page, pageSize);
    }

    /// 排序模式 → ORDER BY 片段;片段全为常量拼接、不嵌入参。动态拼接的 HQL 启动期不做语法校验,
    /// 每个分支都必须有测试真正跑一遍,拼错的分支要等实际命中才会暴露。
    private static String orderHql(SortMode sort) {
        return switch (sort) {
            case NEWEST -> "p.sourcePublishedAt DESC NULLS LAST, p.id DESC";
            case LIKES -> "p.likeCount DESC, p.id DESC";
            case FAVORITES -> "p.favCount DESC, p.id DESC";
            case FEATURED -> "p.id DESC";
        };
    }

    /// 查询已 join-fetch 类目,满足 PromptMapper 对懒加载字段的前置假设;不存在或未发布 → empty。
    @Override
    public Optional<PromptDetail> detail(long id) {
        return prompts.findPublishedWithCategory(id).map(PromptMapper::toDetail);
    }

    /// 三轴(scene/style/subject)类目树,附各类目已发布条目数;计数走一次 GROUP BY 分组统计,
    /// 不逐类目单独查,避免 N+1。轴值不属于这三者的类目静默跳过,不进任何一支列表。
    @Override
    public CategoryTree categories() {
        Map<Long, Long> counts = new HashMap<>();
        for (CategoryJpaRepository.CategoryCountView v : categories.publishedCountByCategory()) {
            counts.put(v.getId(), v.getCnt());
        }
        List<CategoryNode> scene = new ArrayList<>();
        List<CategoryNode> style = new ArrayList<>();
        List<CategoryNode> subject = new ArrayList<>();
        for (CategoryEntity c : categories.findAll(Sort.by("axis", "sort", "id"))) {
            CategoryNode node = new CategoryNode(
                    c.getSlug(), c.getNameZh(), c.getNameEn(),
                    counts.getOrDefault(c.getId(), 0L).intValue());
            switch (c.getAxis()) {
                case "scene" -> scene.add(node);
                case "style" -> style.add(node);
                case "subject" -> subject.add(node);
                default -> { }
            }
        }
        return new CategoryTree(scene, style, subject);
    }
}
