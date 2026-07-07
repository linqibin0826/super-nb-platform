package me.supernb.gallery.domain;

/// 提示词列表排序。featured=收录序(id 倒序);newest=源发布时间倒序;likes/favorites=热度倒序。
public enum SortMode {
    FEATURED,
    NEWEST,
    LIKES,
    FAVORITES;

    /// 解析请求字符串,未知值回退 FEATURED。
    public static SortMode from(String value) {
        if (value == null) {
            return FEATURED;
        }
        return switch (value) {
            case "newest" -> NEWEST;
            case "likes" -> LIKES;
            case "favorites" -> FAVORITES;
            default -> FEATURED;
        };
    }
}
