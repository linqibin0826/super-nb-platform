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
import me.supernb.gallery.domain.port.PromptRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.CategoryJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/// PromptRepository 实现:gallery 库只读查询。
/// 列表的过滤/排序组合是动态 HQL(片段全为常量拼接,参数一律绑定,无注入面)。
@Repository
public class PromptAdapter implements PromptRepository {

    private final PromptJpaRepository prompts;
    private final CategoryJpaRepository categories;
    private final EntityManager em;

    public PromptAdapter(PromptJpaRepository prompts, CategoryJpaRepository categories, EntityManager em) {
        this.prompts = prompts;
        this.categories = categories;
        this.em = em;
    }

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

    private static String orderHql(SortMode sort) {
        return switch (sort) {
            case NEWEST -> "p.sourcePublishedAt DESC NULLS LAST, p.id DESC";
            case LIKES -> "p.likeCount DESC, p.id DESC";
            case FAVORITES -> "p.favCount DESC, p.id DESC";
            case FEATURED -> "p.id DESC";
        };
    }

    @Override
    public Optional<PromptDetail> detail(long id) {
        return prompts.findPublishedWithCategory(id).map(PromptMapper::toDetail);
    }

    @Override
    public CategoryTree categories() {
        Map<Integer, Long> counts = new HashMap<>();
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
