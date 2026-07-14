package me.supernb.activity.infra.adapter.read;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.domain.port.read.AccountAnniversaryReadPort;
import me.supernb.sub2api.account.AccountAgeReadModel;
import org.springframework.stereotype.Component;

/// AccountAnniversaryReadPort 实现:固定 Asia/Shanghai 时区薄委托 AccountAgeReadModel。
@Component
public class AccountAnniversaryReadAdapter implements AccountAnniversaryReadPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AccountAgeReadModel readModel;

    public AccountAnniversaryReadAdapter(AccountAgeReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<Long> registeredExactlyDaysAgo(int days) {
        return readModel.registeredOn(LocalDate.now(ZONE).minusDays(days), ZONE);
    }
}
