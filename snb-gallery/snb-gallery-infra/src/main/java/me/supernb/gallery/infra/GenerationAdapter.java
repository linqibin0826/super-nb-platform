package me.supernb.gallery.infra;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.gallery.app.GenerationRepository;
import me.supernb.gallery.infra.jpa.GenerationEntity;
import me.supernb.gallery.infra.jpa.GenerationImageEntity;
import me.supernb.gallery.infra.jpa.GenerationImageJpaRepository;
import me.supernb.gallery.infra.jpa.GenerationJpaRepository;
import me.supernb.gallery.infra.jpa.GenerationRefJpaRepository;
import me.supernb.gallery.infra.jpa.RefImageEntity;
import me.supernb.gallery.infra.jpa.RefImageId;
import me.supernb.gallery.infra.jpa.RefImageJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// GenerationRepository 实现:生成历史聚合落库/查库/回收键。
///
/// save 幂等:同 id 已存在直接返回其 created_at(首次落库即整单同事务成功,不存在半截单);
/// 参考图去重库 (user_id, sha256) 并发撞 PK 时重试一次——重试轮 exists 命中改走跳过。
@Repository
public class GenerationAdapter implements GenerationRepository {

    private final GenerationJpaRepository generations;
    private final GenerationImageJpaRepository generationImages;
    private final GenerationRefJpaRepository generationRefs;
    private final RefImageJpaRepository refImages;
    private final TransactionTemplate txTemplate;
    private final EntityManager em;

    public GenerationAdapter(GenerationJpaRepository generations,
                             GenerationImageJpaRepository generationImages,
                             GenerationRefJpaRepository generationRefs,
                             RefImageJpaRepository refImages,
                             PlatformTransactionManager txManager,
                             EntityManager em) {
        this.generations = generations;
        this.generationImages = generationImages;
        this.generationRefs = generationRefs;
        this.refImages = refImages;
        this.txTemplate = new TransactionTemplate(txManager);
        this.em = em;
    }

    @Override
    public Optional<Instant> findCreatedAt(String id, long userId) {
        return generations.findByIdAndUserId(id, userId).map(GenerationEntity::getCreatedAt);
    }

    @Override
    public boolean refExists(long userId, String sha256) {
        return refImages.existsById(new RefImageId(userId, sha256));
    }

    @Override
    public Instant save(SaveGeneration c) {
        try {
            return trySave(c);
        } catch (DataIntegrityViolationException e) {
            return trySave(c);
        }
    }

    private Instant trySave(SaveGeneration c) {
        return txTemplate.execute(status -> {
            GenerationEntity existing = em.find(GenerationEntity.class, c.id());
            if (existing != null) {
                return existing.getCreatedAt();
            }
            GenerationEntity g = new GenerationEntity(c.id(), c.userId(), c.prompt(), c.size(), c.n(),
                    c.quality(), c.status(), c.cost(), c.elapsedMs(), c.groupName(), c.keyId(),
                    c.error(), c.thumbKey());
            for (OutputImage o : c.outputs()) {
                g.addImage(o.idx(), o.r2Key(), o.bytes());
            }
            for (RefImage r : c.refs()) {
                RefImageId refId = new RefImageId(c.userId(), r.sha256());
                if (!refImages.existsById(refId)) {
                    refImages.save(new RefImageEntity(refId, r.r2Key(), r.bytes()));
                }
                g.addRef(r.idx(), r.sha256());
            }
            em.persist(g);
            return g.getCreatedAt();
        });
    }

    @Override
    public PageRows list(long userId, int page, int pageSize) {
        Page<GenerationEntity> pg = generations.findByUserId(userId, PageRequest.of(page - 1, pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        // 缩略图回退:thumb_key 为空的存量单,取首张输出图的键(一次查整页,无 N+1)
        List<String> needFallback = pg.getContent().stream()
                .filter(g -> g.getThumbKey() == null).map(GenerationEntity::getId).toList();
        Map<String, String> firstImageKey = new HashMap<>();
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

    @Override
    public Optional<DetailRow> detail(String id, long userId) {
        return generations.findWithImages(id, userId).map(g -> new DetailRow(
                g.getId(), g.getCreatedAt(), g.getPrompt(), g.getSize(), g.getN(), g.getQuality(),
                g.getStatus(), g.getCost(), g.getElapsedMs(), g.getGroupName(), g.getKeyId(), g.getError(),
                g.getImages().stream().map(GenerationImageEntity::getR2Key).toList(),
                generationRefs.refKeysOf(id, userId)));
    }

    @Override
    public Optional<List<String>> deleteReturningObjectKeys(String id, long userId) {
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
