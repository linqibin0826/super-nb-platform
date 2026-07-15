package me.supernb.sub2api.invoice;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [InvoiceOrderReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库(照
/// JdbcRechargeReadModel 惯例:显式 SQL 收敛单文件,测试自建上游表结构钉住假设)。
public class JdbcInvoiceOrderReadModel implements InvoiceOrderReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    /// 构造:注入指向 sub2api 只读源的 JdbcTemplate。
    public JdbcInvoiceOrderReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public List<OrderRow> completedBalanceOrders(long userId) {
        return jdbc.query(
                "SELECT id, out_trade_no, amount, completed_at FROM payment_orders "
                        + "WHERE user_id = :uid AND order_type = 'balance' AND status = 'COMPLETED' "
                        + "ORDER BY completed_at DESC",
                new MapSqlParameterSource("uid", userId),
                (rs, i) -> {
                    long id = rs.getLong("id");
                    String tradeNo = rs.getString("out_trade_no");
                    return new OrderRow(id,
                            tradeNo == null || tradeNo.isBlank() ? String.valueOf(id) : tradeNo,
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("completed_at").toInstant());
                });
    }

    @Override
    public BigDecimal balanceOf(long userId) {
        List<BigDecimal> found = jdbc.query(
                "SELECT balance FROM users WHERE id = :uid",
                new MapSqlParameterSource("uid", userId),
                (rs, i) -> rs.getBigDecimal("balance"));
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ZERO : found.get(0);
    }

    @Override
    public Map<Long, String> emailsByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        jdbc.query("SELECT id, email FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", userIds),
                rs -> {
                    result.put(rs.getLong("id"), rs.getString("email"));
                });
        return result;
    }
}
