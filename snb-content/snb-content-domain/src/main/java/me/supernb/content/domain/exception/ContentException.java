package me.supernb.content.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 内容中心领域异常：文章/分类不存在映射 404，分类仍被引用的拒删映射 409（CONFLICT）。
public class ContentException extends DomainException {

    /// 构造私有：统一经静态工厂创建，trait 随场景给定。
    private ContentException(String message, StandardErrorTrait trait) {
        super(message, trait);
    }

    /// 文章不存在或已下架（对外统一 404，不区分两种情况以免泄露下架状态）。
    public static ContentException articleNotFound(String slug) {
        return new ContentException("文章不存在或已下架: " + slug, StandardErrorTrait.NOT_FOUND);
    }

    /// 发布时引用了不存在的分类（管线侧应先 sync categories.yml）。
    public static ContentException categoryNotFound(String slug) {
        return new ContentException("分类不存在: " + slug, StandardErrorTrait.NOT_FOUND);
    }

    /// 分类仍被文章引用，整表同步拒绝删除（先迁移文章分类再删）。
    public static ContentException categoryInUse(String slug, long articleCount) {
        return new ContentException("分类仍被 " + articleCount + " 篇文章引用，拒绝删除: " + slug,
                StandardErrorTrait.CONFLICT);
    }
}
