package me.supernb.guide.domain.port.repository;

import java.util.Set;

/// 引导已读仓储端口:写(幂等 ack)+ 读(我的已读集合)。表极薄,不再拆读写两 port。
public interface GuideAckRepository {

    /// 标记已读;同 (user,key) 重复调用幂等成功。
    void ack(long userId, String key);

    /// 当前用户全部已读 key。
    Set<String> ackedKeys(long userId);
}
