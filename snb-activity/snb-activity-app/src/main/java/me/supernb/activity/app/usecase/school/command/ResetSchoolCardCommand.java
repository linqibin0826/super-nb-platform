package me.supernb.activity.app.usecase.school.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 消耗一次重置银行,给邀请卡回满额度(Tibo 时刻)。次数服务端推导校验。
public record ResetSchoolCardCommand(long userId) implements Command<SchoolStatusView> {
}
