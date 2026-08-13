package me.supernb.activity.app.usecase.school.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 领取/升级包机邀请卡:应得档位服务端按合格被邀数重算,跨档直升、重复点幂等。
public record ClaimSchoolCardCommand(long userId) implements Command<SchoolStatusView> {
}
