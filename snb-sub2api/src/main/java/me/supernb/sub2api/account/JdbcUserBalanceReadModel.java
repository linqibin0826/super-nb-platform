package me.supernb.sub2api.account;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/// [UserBalanceReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
public class JdbcUserBalanceReadModel implements UserBalanceReadModel {

    private final JdbcTemplate jdbc;

    public JdbcUserBalanceReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BigDecimal balance(long userId) {
        List<BigDecimal> rows = jdbc.query(
                "SELECT balance FROM users WHERE id = ? AND deleted_at IS NULL",
                (rs, i) -> rs.getBigDecimal("balance"), userId);
        return rows.isEmpty() || rows.get(0) == null ? BigDecimal.ZERO : rows.get(0);
    }
}
