package me.supernb.guide.app.usecase.query;

import java.util.Set;
import me.supernb.guide.domain.port.repository.GuideAckRepository;
import org.springframework.stereotype.Service;

/// 已读查询服务:Controller 直接注入。
@Service
public class MyGuideAcksQueryService {

    private final GuideAckRepository acks;

    /// 构造:注入已读仓储端口。
    public MyGuideAcksQueryService(GuideAckRepository acks) {
        this.acks = acks;
    }

    /// 我的全部已读 key。
    public Set<String> ackedKeys(long userId) {
        return acks.ackedKeys(userId);
    }
}
