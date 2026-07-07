package me.supernb.activity.infra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.RechargeQueryPort;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/// activity infra 最小测试装配:自动配置(JPA/Flyway/JdbcTemplate)+ 被测适配器。
/// 充值端口用固定 ¥300 假实现(→ 应得 3 次抽奖),不连 sub2api。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(DrawAdapter.class)
class ActivityInfraTestApp {

    @Bean
    RechargeQueryPort rechargeQueryPort() {
        return new RechargeQueryPort() {
            @Override
            public BigDecimal totalRecharge(long userId, Instant start, Instant end) {
                return new BigDecimal("300");
            }

            @Override
            public List<ActivityDto.LeaderEntry> leaderboard(Instant s, Instant e, int limit) {
                return List.of();
            }

            @Override
            public List<ActivityDto.RechargeEntry> recentRecharges(Instant s, Instant e, int limit) {
                return List.of();
            }

            @Override
            public Map<Long, String> maskedEmailsByIds(Collection<Long> ids) {
                return Map.of();
            }

            @Override
            public Map<String, ActivityDto.CodeStatus> codeStatuses(Collection<String> codes) {
                return Map.of();
            }
        };
    }
}
