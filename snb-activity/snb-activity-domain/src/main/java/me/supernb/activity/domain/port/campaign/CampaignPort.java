package me.supernb.activity.domain.port.campaign;

import java.util.Optional;
import me.supernb.activity.domain.model.Campaign;

/// 活动期查询端口(活动库)。
public interface CampaignPort {

    /// 当前进行中的活动:status='active' 按 id 降序取最新一条;无进行中活动则返回 empty。
    Optional<Campaign> activeCampaign();
}
