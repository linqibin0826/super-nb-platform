package me.supernb.activity.domain.port.read;

import java.time.Instant;
import java.util.Optional;

/// 账号注册时刻只读端口(sub2api 主库):签到账龄门槛用(spec §3.1,注册满 24 小时方可签到,
/// 呼应 07-10 注册即送事故教训)。
public interface AccountRegistrationReadPort {

    /// 用户注册时刻;查无此人返回 empty。
    Optional<Instant> registeredAt(long userId);
}
