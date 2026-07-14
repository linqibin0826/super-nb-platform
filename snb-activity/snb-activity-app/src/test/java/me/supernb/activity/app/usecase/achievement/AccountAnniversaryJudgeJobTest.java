package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.read.AccountAnniversaryReadPort;
import org.junit.jupiter.api.Test;

class AccountAnniversaryJudgeJobTest {

    private final AccountAnniversaryReadPort anniversaryPort = mock(AccountAnniversaryReadPort.class);
    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);

    private AccountAnniversaryJudgeJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new AccountAnniversaryJudgeJob(anniversaryPort, catalogPort, unlockPort, settlementProperties);
    }

    private static AchievementDefinition def(String code, int days) {
        return new AchievementDefinition(code, null, null, "考勤本纪", "T1", 5, false, false, "active",
                "metric_threshold", "account_age_days", new BigDecimal(days), "gte", null,
                LocalDate.of(2026, 7, 13), 0, code, "f", null, "测试条件");
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).judgeDaily();
        verify(anniversaryPort, never()).registeredExactlyDaysAgo(anyInt());
    }

    @Test
    void unlocksUsersRegisteredExactlyOnThreshold() {
        when(catalogPort.activeDefinitions()).thenReturn(List.of(def("account_anniv_1", 100)));
        when(anniversaryPort.registeredExactlyDaysAgo(100)).thenReturn(List.of(42L));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of());

        job(true).judgeDaily();

        verify(unlockPort).unlock(eq(42L), eq("account_anniv_1"), any(), eq(5), eq("batch_scan"));
    }

    @Test
    void alreadyUnlockedUserIsSkipped() {
        when(catalogPort.activeDefinitions()).thenReturn(List.of(def("account_anniv_1", 100)));
        when(anniversaryPort.registeredExactlyDaysAgo(100)).thenReturn(List.of(42L));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of("account_anniv_1"));

        job(true).judgeDaily();

        verify(unlockPort, never()).unlock(eq(42L), eq("account_anniv_1"), any(), anyInt(), any());
    }
}
