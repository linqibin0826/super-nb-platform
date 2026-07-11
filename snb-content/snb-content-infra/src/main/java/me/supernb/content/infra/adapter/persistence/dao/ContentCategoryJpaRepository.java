package me.supernb.content.infra.adapter.persistence.dao;

import me.supernb.content.infra.adapter.persistence.entity.ContentCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 分类 DAO（主键即 slug）。
public interface ContentCategoryJpaRepository extends JpaRepository<ContentCategoryEntity, String> {
}
