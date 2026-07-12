package me.supernb.activity.domain.exception;

/// 金票闸机并发仲裁信号:同一用户当日 attempt 唯一键被并发抢先。
/// 不对外映射 HTTP——由 handler 捕获后以 wantWin=false 换新事务降级重读当日结果。
public class GateAlreadyAttemptedTodayException extends RuntimeException {

    public GateAlreadyAttemptedTodayException() {
        super("gate attempt already recorded for today");
    }
}
