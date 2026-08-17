package me.supernb.ops.domain.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.ops.domain.model.AccountStatus;

/// 邮箱账号聚合持久化端口。password 字段进出都是密文(加密在 app 层做完才传进来)。
public interface OpsAccountRepository {

    /// 账号数据(创建/更新入参与读出共用;passwordEnc/recoveryPasswordEnc 恒为密文或 null)。
    record AccountData(String email, String provider, String passwordEnc, String recoveryEmail,
                       String recoveryPasswordEnc, String regYear, String country, String owner,
                       AccountStatus status, String source, String notes) {
    }

    /// 库中账号行 = id + 数据 + 时间戳。
    record AccountRow(long id, AccountData data, Instant createdAt, Instant updatedAt) {
    }

    long create(AccountData data);

    boolean update(long id, AccountData data);

    boolean delete(long id);

    Optional<AccountRow> find(long id);

    List<AccountRow> listAll();
}
