package me.supernb.guide.infra.adapter.persistence;

import java.util.Set;
import java.util.stream.Collectors;
import me.supernb.guide.domain.port.repository.GuideAckRepository;
import me.supernb.guide.infra.adapter.persistence.dao.GuideAckJpaRepository;
import me.supernb.guide.infra.adapter.persistence.entity.GuideAckEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/// 已读仓储适配:insert 撞 (user,key) 唯一约束=早已读,吞掉即幂等成功。
@Component
public class GuideAckRepositoryAdapter implements GuideAckRepository {

    private final GuideAckJpaRepository dao;

    /// 构造:注入 JPA 仓库。
    public GuideAckRepositoryAdapter(GuideAckJpaRepository dao) {
        this.dao = dao;
    }

    @Override
    public void ack(long userId, String key) {
        try {
            dao.save(new GuideAckEntity(userId, key));
        } catch (DataIntegrityViolationException ignored) {
            // 唯一冲突 = 已经 ack 过,幂等成功
        }
    }

    @Override
    public Set<String> ackedKeys(long userId) {
        return dao.findByUserId(userId).stream().map(GuideAckEntity::getGuideKey).collect(Collectors.toSet());
    }
}
