package me.supernb.invoice.domain.port.repository;

import java.time.Instant;
import java.util.Optional;
import me.supernb.invoice.domain.model.ProfileType;

/// 抬头聚合持久化端口。全部操作按 (userId, profileId) 双键定位——归属校验即隔离。
/// verifiedAt(核验章)由 app 层判定后随写入落库,绝不接受客户端声明。
public interface InvoiceProfileRepository {

    /// 抬头数据(创建/更新入参与快照读出共用一个形状;不含核验章——章另走显式参数,防混入写路径)。
    record ProfileData(ProfileType type, String title, String taxNo, String regAddress,
                       String regPhone, String bankName, String bankAccount) {
    }

    /// 库中抬头 = 数据 + 核验章(verifiedAt 为 null 即未核验)。
    record StoredProfile(ProfileData data, Instant verifiedAt) {
    }

    /// 新建抬头,返回雪花 id。
    long create(long userId, ProfileData data, Instant verifiedAt);

    /// 全量覆盖更新(核验章一并覆盖);false = 不存在或不属于该用户。
    boolean update(long userId, long profileId, ProfileData data, Instant verifiedAt);

    /// 硬删(申请单持有独立快照,删抬头不影响历史);false = 不存在或不属于该用户。
    boolean delete(long userId, long profileId);

    /// 按归属取抬头(申请时做快照、更新时判章用)。
    Optional<StoredProfile> find(long userId, long profileId);

    /// 该用户抬头数(上限守卫用)。
    int countByUser(long userId);
}
