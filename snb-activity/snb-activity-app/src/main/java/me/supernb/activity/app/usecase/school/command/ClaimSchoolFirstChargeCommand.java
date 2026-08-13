package me.supernb.activity.app.usecase.school.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 领取开学季首充礼(档位由服务端按人生首笔付款单重算,不信客户端)。
public record ClaimSchoolFirstChargeCommand(long userId) implements Command<SchoolStatusView> {
}
