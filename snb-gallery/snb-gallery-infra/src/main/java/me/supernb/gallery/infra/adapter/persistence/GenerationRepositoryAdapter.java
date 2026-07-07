package me.supernb.gallery.infra.adapter.persistence;

import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.GenerationImageJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.GenerationJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.GenerationRefJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.RefImageJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationImageEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.RefImageEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// 生成历史聚合仓储适配器:4 表(generation/generation_image/generation_ref/ref_image)
/// 一个事务落库,级联随聚合根。
///
/// 身份即雪花 id(经 [#nextId()] 预分配,插入不会撞主键);
/// 撞唯一约束仅剩参考图 (user_id, sha256) 并发去重一种,重试一轮改走跳过。
@Repository
public class GenerationRepositoryAdapter implements GenerationRepository {

    private final GenerationJpaRepository generations;
    private final GenerationImageJpaRepository generationImages;
    private final GenerationRefJpaRepository generationRefs;
    private final RefImageJpaRepository refImages;
    private final TransactionTemplate txTemplate;

    /// 构造:注入 4 张表的 Spring Data 仓储与事务模板。
    public GenerationRepositoryAdapter(GenerationJpaRepository generations,
                             GenerationImageJpaRepository generationImages,
                             GenerationRefJpaRepository generationRefs,
                             RefImageJpaRepository refImages,
                             PlatformTransactionManager txManager) {
        this.generations = generations;
        this.generationImages = generationImages;
        this.generationRefs = generationRefs;
        this.refImages = refImages;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// 预分配下一个生成记录 id(雪花)。
    @Override
    public long nextId() {
        return SnowflakeIdGenerator.getId();
    }

    /// 用户参考图库中是否已有该内容哈希。
    @Override
    public boolean refExists(long userId, String sha256) {
        return refImages.existsByUserIdAndSha256(userId, sha256);
    }

    /// 落库:参考图 (user_id, sha256) 并发去重撞唯一约束时重试一次(重试轮 exists 命中改走跳过)。
    @Override
    public Instant save(SaveGeneration c) {
        try {
            return trySave(c);
        } catch (DataIntegrityViolationException e) {
            return trySave(c);
        }
    }

    /// 事务体:组装聚合(输出图/参考图去重/引用)一次持久化。
    private Instant trySave(SaveGeneration c) {
        return txTemplate.execute(status -> {
            GenerationEntity g = new GenerationEntity(c.id(), c.userId(), c.prompt(), c.size(), c.n(),
                    c.quality(), c.status(), c.cost(), c.elapsedMs(), c.groupName(), c.keyId(),
                    c.error(), c.thumbKey());
            for (OutputImage o : c.outputs()) {
                g.addImage(o.idx(), o.r2Key(), o.bytes());
            }
            for (RefImage r : c.refs()) {
                if (!refImages.existsByUserIdAndSha256(c.userId(), r.sha256())) {
                    refImages.save(new RefImageEntity(c.userId(), r.sha256(), r.r2Key(), r.bytes()));
                }
                g.addRef(r.idx(), r.sha256());
            }
            generations.save(g);
            return g.getCreatedAt();
        });
    }

    /// 用户生成历史分页(按创建时刻倒序),缩略图缺失的存量单回退首图键(整页一次批查,无 N+1)。
    @Override
    public PageRows list(long userId, int page, int pageSize) {
        Page<GenerationEntity> pg = generations.findByUserId(userId, PageRequest.of(page - 1, pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        // 缩略图回退:thumb_key 为空的存量单,取首张输出图的键(一次查整页,无 N+1)
        List<Long> needFallback = pg.getContent().stream()
                .filter(g -> g.getThumbKey() == null).map(GenerationEntity::getId).toList();
        Map<Long, String> firstImageKey = new HashMap<>();
        if (!needFallback.isEmpty()) {
            for (GenerationImageEntity img : generationImages.findByGeneration_IdInOrderByIdxAsc(needFallback)) {
                firstImageKey.putIfAbsent(img.getGeneration().getId(), img.getR2Key());
            }
        }

        List<ListRow> rows = pg.getContent().stream().map(g -> new ListRow(
                g.getId(), g.getCreatedAt(), g.getPrompt(), g.getSize(), g.getN(), g.getQuality(),
                g.getStatus(), g.getCost(), g.getElapsedMs(), g.getError(),
                g.getThumbKey() != null ? g.getThumbKey() : firstImageKey.get(g.getId()))).toList();
        return new PageRows(rows, pg.getTotalElements());
    }

    /// 单条详情:输出图预取 + 参考图键内容寻址联查;不存在或不归属该用户返回 empty。
    @Override
    public Optional<DetailRow> detail(long id, long userId) {
        return generations.findWithImages(id, userId).map(g -> new DetailRow(
                g.getId(), g.getCreatedAt(), g.getPrompt(), g.getSize(), g.getN(), g.getQuality(),
                g.getStatus(), g.getCost(), g.getElapsedMs(), g.getGroupName(), g.getKeyId(), g.getError(),
                g.getImages().stream().map(GenerationImageEntity::getR2Key).toList(),
                generationRefs.refKeysOf(g.getId(), userId)));
    }

    /// 删除生成记录并返回其全部 R2 对象键(含缩略图);不存在返回 empty,由用例转 404。
    @Override
    public Optional<List<String>> deleteReturningObjectKeys(long id, long userId) {
        return txTemplate.execute(status -> {
            GenerationEntity g = generations.findWithImages(id, userId).orElse(null);
            if (g == null) {
                return Optional.empty();
            }
            List<String> keys = new ArrayList<>(
                    g.getImages().stream().map(GenerationImageEntity::getR2Key).toList());
            if (g.getThumbKey() != null) {
                keys.add(g.getThumbKey());
            }
            generations.delete(g);
            return Optional.of(keys);
        });
    }
}
