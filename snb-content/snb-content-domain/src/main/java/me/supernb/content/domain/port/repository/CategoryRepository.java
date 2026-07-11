package me.supernb.content.domain.port.repository;

import java.util.List;

/// 分类的写端口：内容仓库 categories.yml 整表同步 + 存在性守卫。
public interface CategoryRepository {

    /// 分类是否存在（upsert 文章前的守卫）。
    boolean exists(String slug);

    /// 整表对齐：incoming 逐个 upsert；现存但不在 incoming 的分类，仍被文章引用则拒删（抛
    /// [me.supernb.content.domain.exception.ContentException]，事务回滚原样保留），否则删除。
    SyncOutcome sync(List<CategoryData> categories);

    /// 一条分类配置（categories.yml 的一项）。
    record CategoryData(String slug, String name, int sortOrder) {}

    /// 同步结果：upsert 条数 + 删除条数。
    record SyncOutcome(int upserted, int deleted) {}
}
