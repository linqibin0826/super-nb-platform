package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/// raffle infra 最小测试装配:自动配置(JPA/Flyway/JdbcTemplate)+ 被测适配器。
/// 门槛端口用可变 map 桩(测试直接改静态字段模拟复核时指标涨跌),不连 sub2api。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({RaffleCampaignAdapter.class, RaffleEntryAdapter.class, RafflePrizeAdapter.class, RaffleDrawAdapter.class})
class RaffleInfraTestApp {

    /// 可变门槛桩:userId -> 指标值;测试用例按需增删改。
    static final Map<Long, BigDecimal> GATE_VALUES = new HashMap<>();

    /// 可变注册时刻桩:缺席=查无此人。
    static final Map<Long, Instant> REGISTERED_ATS = new HashMap<>();

    @Bean
    RaffleGateReadPort raffleGateReadPort() {
        return new RaffleGateReadPort() {
            @Override
            public BigDecimal gateValue(long userId, GateType type, Instant from, Instant to) {
                return GATE_VALUES.getOrDefault(userId, BigDecimal.ZERO);
            }

            @Override
            public Map<Long, BigDecimal> gateValues(Collection<Long> userIds, GateType type,
                    Instant from, Instant to) {
                return userIds.stream()
                        .filter(GATE_VALUES::containsKey)
                        .collect(Collectors.toMap(u -> u, GATE_VALUES::get));
            }

            @Override
            public Map<Long, Instant> registeredAts(Collection<Long> userIds) {
                return userIds.stream()
                        .filter(REGISTERED_ATS::containsKey)
                        .collect(Collectors.toMap(u -> u, REGISTERED_ATS::get));
            }

            @Override
            public Map<Long, String> displayNames(Collection<Long> userIds) {
                return userIds.stream().collect(Collectors.toMap(u -> u, u -> "用户" + u));
            }
        };
    }
}
