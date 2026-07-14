package me.supernb.activity.app.usecase.checkin.config;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// 签到功能上线日期(spec §3.1,红一④/红二9):满勤统计的分母窗口不得早于此日期,
/// 历史记录不计入分母。值来自 `activity.checkin.launch-date`(`CHECKIN_LAUNCH_DATE`)。
@Component
public class CheckinProperties {

    private final LocalDate launchDate;

    /// 构造:@Value 注入(app 模块只依赖 spring-context,不用 @ConfigurationProperties,
    /// 照 GateProperties/ReferralProperties 惯例)。
    public CheckinProperties(@Value("${activity.checkin.launch-date:2026-07-13}") String launchDate) {
        this.launchDate = LocalDate.parse(launchDate);
    }

    /// 签到功能上线日期(Asia/Shanghai 自然日)。
    public LocalDate launchDate() {
        return launchDate;
    }
}
