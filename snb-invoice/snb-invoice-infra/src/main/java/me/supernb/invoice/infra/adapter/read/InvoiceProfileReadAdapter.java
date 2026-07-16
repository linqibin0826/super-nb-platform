package me.supernb.invoice.infra.adapter.read;

import java.util.List;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.ProfileView;
import me.supernb.invoice.domain.port.read.InvoiceProfileReadPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// 抬头只读投影(主数据源 JdbcTemplate,创建时间正序)。
@Component
public class InvoiceProfileReadAdapter implements InvoiceProfileReadPort {

    private final JdbcTemplate jdbc;

    /// 构造:注入主数据源 JdbcTemplate。
    public InvoiceProfileReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ProfileView> listByUser(long userId) {
        return jdbc.query(
                "SELECT id, type, title, tax_no, reg_address, reg_phone, bank_name, bank_account, verified_at "
                        + "FROM invoice.invoice_profile WHERE user_id = ? ORDER BY created_at, id",
                (rs, i) -> new ProfileView(rs.getLong("id"), ProfileType.valueOf(rs.getString("type")),
                        rs.getString("title"), rs.getString("tax_no"), rs.getString("reg_address"),
                        rs.getString("reg_phone"), rs.getString("bank_name"), rs.getString("bank_account"),
                        rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toInstant()),
                userId);
    }
}
