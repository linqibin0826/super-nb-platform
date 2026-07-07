package me.supernb.activity.app;

import java.util.Optional;
import me.supernb.activity.domain.Campaign;

/// 活动期查询端口(活动库)。
public interface CampaignPort {

    /// 当前进行中的活动(status='active',取最新一期);无则 empty。
    Optional<Campaign> activeCampaign();
}
