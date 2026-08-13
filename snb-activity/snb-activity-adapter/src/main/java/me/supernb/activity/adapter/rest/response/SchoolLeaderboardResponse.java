package me.supernb.activity.adapter.rest.response;

import java.util.List;
import me.supernb.activity.app.usecase.school.query.SchoolLeaderboardQueryService;

/// 开学季拉人榜响应(公开免登录):name 已在读模型内脱敏,rank 由本层按序补号。
/// 收官/休眠时 entries 为空数组。
public record SchoolLeaderboardResponse(List<Entry> entries) {

    /// 榜单条目。
    public record Entry(int rank, String name, int count) {
    }

    public static SchoolLeaderboardResponse of(List<SchoolLeaderboardQueryService.Entry> top) {
        List<Entry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            entries.add(new Entry(i + 1, top.get(i).name(), top.get(i).count()));
        }
        return new SchoolLeaderboardResponse(List.copyOf(entries));
    }
}
