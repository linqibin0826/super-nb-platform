package me.supernb.gallery.app;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// studio 生成历史仓储端口(gallery 库,4 表)。R2 上传/签名由服务经 ImageStoragePort 完成,
/// 本端口只落库/查库/回收键。
public interface GenerationRepository {

    /// 一次生成的持久化输入(R2 键已由服务算好/上传好)。
    record SaveGeneration(
            String id, long userId, String prompt, String size, int n, String quality, String status,
            Double cost, int elapsedMs, String groupName, Long keyId, String error, String thumbKey,
            List<OutputImage> outputs, List<RefImage> refs) {
    }

    /// 输出图。
    record OutputImage(int idx, String r2Key, int bytes) {
    }

    /// 参考图(内容寻址,key 由 sha 决定;bytes 仅新图有意义)。
    record RefImage(int idx, String sha256, String r2Key, int bytes) {
    }

    /// 列表行(thumbKey 已解析:缩略图优先,回退首图;可能为 null)。
    record ListRow(
            String id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String error, String thumbKey) {
    }

    /// 分页行集。
    record PageRows(List<ListRow> rows, long total) {
    }

    /// 详情行(含全部输出/参考图 R2 键)。
    record DetailRow(
            String id, Instant createdAt, String prompt, String size, int n, String quality,
            String status, Double cost, int elapsedMs, String groupName, Long keyId, String error,
            List<String> outputKeys, List<String> refKeys) {
    }

    /// 幂等检查:本人已有该 id 则回其 createdAt。
    Optional<Instant> findCreatedAt(String id, long userId);

    /// 某用户是否已存过该 sha 的参考图(去重依据)。
    boolean refExists(long userId, String sha256);

    /// 落库(4 表,一个事务),回 createdAt。
    Instant save(SaveGeneration cmd);

    /// 我的生成历史分页(倒序)。
    PageRows list(long userId, int page, int pageSize);

    /// 单条详情;非本人/不存在 → empty。
    Optional<DetailRow> detail(String id, long userId);

    /// 删主行(级联 images/refs)并回收需清理的 R2 键(输出图 + 缩略图);非本人/不存在 → empty。
    Optional<List<String>> deleteReturningObjectKeys(String id, long userId);
}
