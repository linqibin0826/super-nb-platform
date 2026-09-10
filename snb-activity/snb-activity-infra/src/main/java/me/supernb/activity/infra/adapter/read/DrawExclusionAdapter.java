package me.supernb.activity.infra.adapter.read;

import java.util.Arrays;
import java.util.List;
import me.supernb.activity.domain.port.draw.DrawExclusionPort;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// DrawExclusionPort 实现:`activity.draw.excluded-group-ids`(env `DRAW_EXCLUDED_GROUP_IDS`,逗号分隔
/// 分组 id)列出的分组,凡在 sub2api `user_allowed_groups` 里被授权了其一的用户即被排除。
/// 名单为空 = 规则休眠,一次 SQL 都不发(现状不变)。生产填 146 = 「Claude-Max-下游专属」1.0x 组。
///
/// ⚠️ compose 不加 environment 映射 = 这里的默认值(空)静默生效,规则等于没开(GATE_ 老教训)。
@Component
public class DrawExclusionAdapter implements DrawExclusionPort {

    private final List<Long> excludedGroupIds;
    private final RechargeReadModel readModel;

    /// 构造:解析逗号分隔的分组 id(容忍空白与尾逗号),注入 sub2api 只读模型。
    public DrawExclusionAdapter(@Value("${activity.draw.excluded-group-ids:}") String excludedGroupIdsCsv,
                                RechargeReadModel readModel) {
        this.excludedGroupIds = Arrays.stream(excludedGroupIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        this.readModel = readModel;
    }

    /// 名单空恒 false;否则问读模型该用户有没有被授权名单里的任一分组。
    @Override
    public boolean isExcluded(long userId) {
        return !excludedGroupIds.isEmpty() && readModel.inAnyAllowedGroup(userId, excludedGroupIds);
    }

    /// 解析后的排除分组 id 列表(只读;测试与启动日志用)。
    public List<Long> excludedGroupIds() {
        return excludedGroupIds;
    }
}
