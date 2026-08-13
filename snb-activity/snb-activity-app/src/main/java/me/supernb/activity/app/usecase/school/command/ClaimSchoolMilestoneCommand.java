package me.supernb.activity.app.usecase.school.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.app.usecase.school.SchoolStatusView;

/// 领取开学季带人里程碑卡。tier=人数档(1/3/6);KFC 档(10)人工私聊发放,无此命令。
public record ClaimSchoolMilestoneCommand(long userId, int tier) implements Command<SchoolStatusView> {
}
