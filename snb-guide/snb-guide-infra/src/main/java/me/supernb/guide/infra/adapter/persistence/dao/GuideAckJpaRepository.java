package me.supernb.guide.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.guide.infra.adapter.persistence.entity.GuideAckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// guide_ack Spring Data 仓库。
public interface GuideAckJpaRepository extends JpaRepository<GuideAckEntity, Long> {

    /// 按用户取全部已读行。
    List<GuideAckEntity> findByUserId(long userId);
}
