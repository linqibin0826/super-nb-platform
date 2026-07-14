package me.supernb.activity.domain.port.read;

import java.util.List;

/// 造像车间成就信号只读端口(同库跨 gallery schema 原生 SQL,不建 Java 编译期依赖)。
public interface GalleryAchievementSignalPort {

    /// 用户 id + 计数。
    record UserCount(long userId, long count) {
    }

    /// 全体用户累计成功生成数(gallery.generation,status='done')。
    List<UserCount> generationDoneCounts();

    /// 全体用户点赞+收藏合计(两表相加;两表本无取消语义,COUNT(*) 即"当前有效")。
    List<UserCount> likeAndFavoriteCounts();
}
