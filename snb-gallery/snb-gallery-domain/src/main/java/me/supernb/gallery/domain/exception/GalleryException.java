package me.supernb.gallery.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 灵感库领域异常,统一映射 HTTP 404(NOT_FOUND)。提示词不存在/未发布,或生成记录不存在/不归属当前用户时抛出。
///
/// 提示词与生成记录两类 404 场景共用同一个类,经不同静态工厂区分,没必要拆成两个异常类型。
public class GalleryException extends DomainException {

    /// 构造私有:统一经下方静态工厂创建,trait 恒为 NOT_FOUND。
    private GalleryException(String message) {
        super(message, StandardErrorTrait.NOT_FOUND);
    }

    /// 404:提示词不存在或未发布。
    public static GalleryException promptNotFound(long id) {
        return new GalleryException("提示词不存在或未发布: " + id);
    }

    /// 404:生成记录不存在或不归属当前用户。
    public static GalleryException generationNotFound(String id) {
        return new GalleryException("生成记录不存在: " + id);
    }
}
