package me.supernb.invoice.app.usecase.support;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/// 双层日配额(单用户 + 全站,自然日,内存计数):付费出站能力的烧钱保护。
/// 重启清零可接受——这是保护不是计费;超限抛调用方提供的异常(各能力口径不同)。
public class DailyQuota {

    private final int userDaily;
    private final int globalDaily;

    private final Map<Long, Integer> userCounts = new HashMap<>();
    private LocalDate day = LocalDate.now();
    private int globalCount;

    /// 构造:单用户日限 + 全站日限。
    public DailyQuota(int userDaily, int globalDaily) {
        this.userDaily = userDaily;
        this.globalDaily = globalDaily;
    }

    /// 消耗一次配额;单用户或全站任一超限即抛 onExceeded 提供的异常,不消耗。
    public synchronized void consume(long userId, Supplier<? extends RuntimeException> onExceeded) {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {
            day = today;
            globalCount = 0;
            userCounts.clear();
        }
        int mine = userCounts.getOrDefault(userId, 0);
        if (mine >= userDaily || globalCount >= globalDaily) {
            throw onExceeded.get();
        }
        userCounts.put(userId, mine + 1);
        globalCount++;
    }
}
