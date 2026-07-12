package me.supernb.sub2api.gate;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;

/// [GateReadModel] 的 JDBC 实现(sub2api 主库只读源)。
public class JdbcGateReadModel implements GateReadModel {

    private final JdbcTemplate jdbc;

    public JdbcGateReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BigDecimal totalRecharged(long userId) {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payment_orders "
                        + "WHERE user_id = ? AND status = 'COMPLETED' AND order_type = 'balance'",
                BigDecimal.class, userId);
        return total == null ? BigDecimal.ZERO : total;
    }
}
