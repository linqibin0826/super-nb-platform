package me.supernb.sub2api.account;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/// sub2api 账号年龄只读读模型:批量"注册于某个自然日"查询,account_anniv 成就
/// (百日报到/周年机长)判定的候选发现用——account_age_days 本身"判定时现算,纯函数不入表"
/// (深化稿 §6.1),本模型只回答"今天有谁刚好满 N 天注册"这一个问题。
public interface AccountAgeReadModel {

    /// localDate 在 zone 时区的自然日窗口 [00:00, 次日00:00) 内注册的用户 id。
    List<Long> registeredOn(LocalDate localDate, ZoneId zone);
}
