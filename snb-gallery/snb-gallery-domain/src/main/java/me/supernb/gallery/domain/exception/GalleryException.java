package me.supernb.gallery.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 灵感库领域异常(NOT_FOUND → 404)。提示词/生成记录不存在或未发布时抛出。
public class GalleryException extends DomainException {

    /// 统一走静态工厂构造。
    private GalleryException(String message) {
        super(message, StandardErrorTrait.NOT_FOUND);
    }

    /// 404:提示词不存在或未发布。
    public static GalleryException promptNotFound(long id) {
        return new GalleryException("提示词不存在或未发布: " + id);
    }

    /// 404:生成记录不存在或非本人。
    public static GalleryException generationNotFound(String id) {
        return new GalleryException("生成记录不存在: " + id);
    }
}
