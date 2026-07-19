package me.supernb.activity.domain.port.read;

import java.util.List;
import me.supernb.activity.domain.model.read.raffle.SubscriptionGroupView;

/// 订阅分组读端口:列出可作为兑换码生成目标的 sub2api 分组(管理端下拉数据源)。
public interface SubscriptionGroupReadPort {

    /// 全部活跃订阅分组,按上游顺序;admin 通道未配置时抛通道不可用异常。
    List<SubscriptionGroupView> listForRedeemCode();
}
