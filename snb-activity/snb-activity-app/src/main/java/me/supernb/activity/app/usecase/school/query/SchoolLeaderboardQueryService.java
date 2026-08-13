package me.supernb.activity.app.usecase.school.query;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import org.springframework.stereotype.Service;

/// 开学季拉人榜查询(公开端点内舱)。name 在读模型内已脱敏,本层只透传;
/// 榜单计数不封顶(里程碑封 10 是奖励口径,榜单是竞争口径,两者刻意不同)。
/// 宽限期榜单仍可见(资格边界钉 [start,end),收官后数字自然冻结)。
@Service
public class SchoolLeaderboardQueryService {

    private static final int BOARD_SIZE = 20;

    private final SchoolSeasonProperties props;
    private final SchoolReadPort readPort;

    /// 构造:注入活动配置与只读端口。
    public SchoolLeaderboardQueryService(SchoolSeasonProperties props, SchoolReadPort readPort) {
        this.props = props;
        this.readPort = readPort;
    }

    /// Top 20 拉人榜(收官榜奖仍只发前 10,11~20 是候补席);休眠/未开始/领取截止后返回空列表。
    public List<Entry> top(Instant now) {
        if (!props.configured() || now.isBefore(props.start()) || !now.isBefore(props.claimDeadline())) {
            return List.of();
        }
        return readPort.topInviters(props.start(), props.end(),
                        SchoolSeasonProperties.INVITEE_MIN_CNY, BOARD_SIZE)
                .stream()
                .map(r -> new Entry(r.name(), r.count(), r.invited()))
                .toList();
    }

    /// 榜单条目(name 已脱敏)。count=首充合格数(排名口径);invited=注册总数(展示)。
    public record Entry(String name, int count, int invited) {
    }
}
