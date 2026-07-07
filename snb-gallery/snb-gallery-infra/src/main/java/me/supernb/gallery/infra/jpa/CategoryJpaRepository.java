package me.supernb.gallery.infra.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// gallery.category 仓储。
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Integer> {

    /// 每类目已发布提示词计数(无条目的类目不出现,适配层补 0)。
    @Query("SELECT p.category.id AS id, COUNT(p) AS cnt FROM PromptEntity p "
            + "WHERE p.status = 'published' AND p.category IS NOT NULL GROUP BY p.category.id")
    List<CategoryCountView> publishedCountByCategory();

    /// 类目计数投影。
    interface CategoryCountView {
        Integer getId();

        long getCnt();
    }
}
