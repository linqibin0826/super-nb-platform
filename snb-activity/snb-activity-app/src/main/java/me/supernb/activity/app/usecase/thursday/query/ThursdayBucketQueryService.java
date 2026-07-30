package me.supernb.activity.app.usecase.thursday.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
        return new ThursdayBucketView(true, eligible, claimed, eligible ? idx + 1 : null,
                qualified.size(), props.bucketLimit());
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
