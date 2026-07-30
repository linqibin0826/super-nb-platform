package me.supernb.activity.app.usecase.thursday.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.app.usecase.thursday.HiddenBucketDraw;
import me.supernb.activity.app.usecase.thursday.ThursdayBucketView;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.domain.port.read.ThursdayBucketReadPort;
import org.springframework.stereotype.Service;

/// 疯四桶状态查询(spec §3)。资格与桶序全在服务端算,时区显式 Asia/Shanghai——
/// 前端不得用 `new Date()` 自己推"今天是不是周四"(签到 spec §7.3 同款纪律:
/// 客户端时钟不可信,用户改系统时间就能骗出一个不存在的场次)。
///
/// 领取命令与本查询共用这一份判定,避免两处口径漂移。
@Service
public class ThursdayBucketQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ThursdayProperties props;
    private final ThursdayBucketReadPort readPort;

    /// 构造:注入疯四场次配置与只读端口。
    public ThursdayBucketQueryService(ThursdayProperties props, ThursdayBucketReadPort readPort) {
        this.props = props;
        this.readPort = readPort;
    }

    /// 本人在今天这一场的视图;今天不是场次(或未配分组)返回休眠态。
    public ThursdayBucketView view(long userId) {
        LocalDate today = LocalDate.now(ZONE);
        Long groupId = props.groupIdFor(today);
        if (groupId == null) {
            return ThursdayBucketView.closed(props.bucketLimit());
        }
        List<Long> qualified = qualifiedToday(today);
        int idx = qualified.indexOf(userId);
        boolean eligible = idx >= 0;
        boolean claimed = eligible && readPort.alreadyClaimed(userId, groupId, props.notes());
        Integer bucketNo = eligible ? idx + 1 : null;
        List<Integer> hidden = hiddenBuckets(today);
        boolean hiddenWin = hidden != null && bucketNo != null && hidden.contains(bucketNo);
        return new ThursdayBucketView(true, eligible, claimed, bucketNo,
                qualified.size(), props.bucketLimit(), hidden, hiddenWin);
    }

    /// 隐藏款(瑞幸)中奖桶序;**未到开奖时刻返回 null**(前端据此显示"待开奖")。
    ///
    /// 抽签范围锁在「开奖时刻的桶数」——窗口是 [当天 00:00, 开奖时刻),不是整天。
    /// 开奖后这个数就不再变,所以号也不再变;开奖后才充值进来的人拿得到桶、但不参与本场隐藏款
    /// (与运营脚本 22:00 批扫的名单口径一致)。
    public List<Integer> hiddenBuckets(LocalDate day) {
        return hiddenBuckets(day, Instant.now());
    }

    /// 同上,`now` 显式传入——留这道缝是为了让"开奖前后"两条分支都能被单测钉住;
    /// 否则测试只能靠真实时钟,一到 22:00 就自己变红(时间型 flaky)。生产代码只走上面那个重载。
    public List<Integer> hiddenBuckets(LocalDate day, Instant now) {
        Instant reveal = day.atTime(props.revealAt()).atZone(ZONE).toInstant();
        if (now.isBefore(reveal)) {
            return null;
        }
        Instant start = day.atStartOfDay(ZONE).toInstant();
        int frozen = readPort.qualifiedInOrder(start, reveal, props.minAmountCny(), props.bucketLimit()).size();
        return HiddenBucketDraw.draw(props.hiddenSalt(), day, frozen, props.hiddenCount());
    }

    /// 今天这一场的资格名单(到账顺序,已封顶)。
    public List<Long> qualifiedToday(LocalDate day) {
        Instant start = day.atStartOfDay(ZONE).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZONE).toInstant();
        return readPort.qualifiedInOrder(start, end, props.minAmountCny(), props.bucketLimit());
    }

    /// 今天的场次分组 id(领取命令用);非场次日为 null。
    public Long groupIdToday() {
        return props.groupIdFor(LocalDate.now(ZONE));
    }

    /// 今天(Asia/Shanghai)的日期——领取命令与本查询共用同一个"今天",防跨零点错场。
    public LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
