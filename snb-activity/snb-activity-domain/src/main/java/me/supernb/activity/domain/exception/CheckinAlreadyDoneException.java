package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 今日已打过卡 → 409(CONFLICT——"与现有资源冲突"精确匹配"重复创建"语义;前端接线计划契约:
/// 幂等冲突场景,例如双标签页/双击竞态,前端收到 409 后重新拉取一次 GET status 兜底同步)。
/// 数据库写入路径本身仍是幂等的 INSERT ON CONFLICT DO NOTHING(Task 2,不因本异常改变),
/// 本异常只影响 API 语义层"要不要把重复调用当错误报给调用方"这一选择。
public class CheckinAlreadyDoneException extends DomainException {

    /// 固定文案构造。
    public CheckinAlreadyDoneException() {
        super("今日已签到", StandardErrorTrait.CONFLICT);
    }
}
