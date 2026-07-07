package me.supernb.gallery.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.repository.InteractionRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptFavoriteJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptLikeJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.entity.InteractionId;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptFavoriteEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptLikeEntity;
import me.supernb.gallery.infra.adapter.read.PromptMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// InteractionRepository 实现:点赞/收藏。
///
/// toggle 语义:成员变更与计数 ±1 同一事务,幂等、计数不为负、只认 published;
/// 目标不存在 → 空(服务转 404)。并发同键插入撞 PK 时整个事务回滚(计数一并还原),
/// 外层按「目标态已达成」回读计数返回。退出侧用批量 DELETE 的真实行数决定是否减计数,
/// 行锁保证并发双删只有一方计入。
@Repository
public class InteractionRepositoryAdapter implements InteractionRepository {

    private static final String PUBLISHED = "published";

    private final PromptJpaRepository prompts;
    private final PromptLikeJpaRepository likes;
    private final PromptFavoriteJpaRepository favorites;
    private final TransactionTemplate txTemplate;

    public InteractionRepositoryAdapter(PromptJpaRepository prompts, PromptLikeJpaRepository likes,
                              PromptFavoriteJpaRepository favorites, PlatformTransactionManager txManager) {
        this.prompts = prompts;
        this.likes = likes;
        this.favorites = favorites;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// 点赞开关(事务 + 撞 PK 幂等回读);目标不存在 → empty。
    @Override
    public OptionalInt toggleLike(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleLike(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.likeCountOf(promptId));
        }
    }

    /// 收藏开关(事务 + 撞 PK 幂等回读);目标不存在 → empty。
    @Override
    public OptionalInt toggleFavorite(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleFavorite(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.favCountOf(promptId));
        }
    }

    /// 事务体:成员表增删 + 计数原子增减。
    private OptionalInt doToggleLike(long promptId, long userId, boolean on) {
        if (!prompts.existsByIdAndStatus(promptId, PUBLISHED)) {
            return OptionalInt.empty();
        }
        if (on) {
            InteractionId key = new InteractionId(promptId, userId);
            if (!likes.existsById(key)) {
                likes.save(new PromptLikeEntity(key));
                prompts.adjustLikeCount(promptId, 1);
            }
        } else if (likes.deleteMembership(promptId, userId) > 0) {
            prompts.adjustLikeCount(promptId, -1);
        }
        return currentCount(prompts.likeCountOf(promptId));
    }

    /// 事务体:成员表增删 + 计数原子增减。
    private OptionalInt doToggleFavorite(long promptId, long userId, boolean on) {
        if (!prompts.existsByIdAndStatus(promptId, PUBLISHED)) {
            return OptionalInt.empty();
        }
        if (on) {
            InteractionId key = new InteractionId(promptId, userId);
            if (!favorites.existsById(key)) {
                favorites.save(new PromptFavoriteEntity(key));
                prompts.adjustFavCount(promptId, 1);
            }
        } else if (favorites.deleteMembership(promptId, userId) > 0) {
            prompts.adjustFavCount(promptId, -1);
        }
        return currentCount(prompts.favCountOf(promptId));
    }

    /// Optional<Integer> → OptionalInt。
    private static OptionalInt currentCount(Optional<Integer> count) {
        return count.map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    @Override
    public Page<PromptSummary> myFavorites(long userId, int page, int pageSize) {
        org.springframework.data.domain.Page<PromptEntity> rows = favorites.favoritePrompts(userId, PageRequest.of(page - 1, pageSize));
        return Page.of(
                rows.getContent().stream().map(PromptMapper::toSummary).toList(),
                rows.getTotalElements(), page, pageSize);
    }

    /// 批量查我在这批 id 上的赞/藏。
    @Override
    public MyInteractions myInteractions(List<Long> promptIds, long userId) {
        if (promptIds.isEmpty()) {
            return new MyInteractions(List.of(), List.of());
        }
        List<Long> liked = likes.findById_UserIdAndId_PromptIdIn(userId, promptIds).stream()
                .map(l -> l.getId().getPromptId()).toList();
        List<Long> favorited = favorites.findById_UserIdAndId_PromptIdIn(userId, promptIds).stream()
                .map(f -> f.getId().getPromptId()).toList();
        return new MyInteractions(liked, favorited);
    }
}
