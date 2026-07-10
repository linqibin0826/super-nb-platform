package me.supernb.content.infra.adapter.persistence.dao;

import me.supernb.content.infra.adapter.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 分类 DAO（主键即 slug）。
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, String> {
}
