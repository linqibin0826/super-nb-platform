package me.supernb.activity.infra.adapter.scan;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// ScanWatermarkPort 实现:纯 JdbcTemplate 自然键 upsert。
@Component
public class JdbcScanWatermarkAdapter implements ScanWatermarkPort {

    private final JdbcTemplate jdbc;

    /// 构造:注入 Boot 主数据源的 JdbcTemplate。
    public JdbcScanWatermarkAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Instant> get(String jobName) {
        List<Instant> rows = jdbc.query(
                "SELECT watermark FROM activity.checkin_scan_watermark WHERE job_name = ?",
                (rs, i) -> rs.getTimestamp("watermark").toInstant(), jobName);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public void advance(String jobName, Instant to) {
        jdbc.update("INSERT INTO activity.checkin_scan_watermark (job_name, watermark) VALUES (?, ?) "
                        + "ON CONFLICT (job_name) DO UPDATE SET watermark = EXCLUDED.watermark, updated_at = now()",
                jobName, Timestamp.from(to));
    }
}
