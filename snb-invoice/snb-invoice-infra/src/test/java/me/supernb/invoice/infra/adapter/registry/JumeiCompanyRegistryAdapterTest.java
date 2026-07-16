package me.supernb.invoice.infra.adapter.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.infra.adapter.registry.JumeiCompanyRegistryAdapter.JumeiData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 纯逻辑面:地址电话混排拆分、数据体清洗、appcode 缺席的关闭姿态。HTTP 面不在单测里打
/// (供应商真接口按次付费),以本地端到端实测为准。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class JumeiCompanyRegistryAdapterTest {

    @Test
    void splitsTrailingPhonesFromAddress() {
        String[] out = JumeiCompanyRegistryAdapter.splitAddressPhone(
                "深圳市南山区高新区科技中一路腾讯大厦35层0755-86013388 0755-86103388");
        assertThat(out[0]).isEqualTo("深圳市南山区高新区科技中一路腾讯大厦35层");
        assertThat(out[1]).isEqualTo("0755-86013388 0755-86103388");
    }

    @Test
    void shortTrailingDigitsStayInAddress() {
        // 门牌号不是电话:尾部数字串不足 8 位不拆
        String[] out = JumeiCompanyRegistryAdapter.splitAddressPhone("杭州市西湖区某某路88");
        assertThat(out[0]).isEqualTo("杭州市西湖区某某路88");
        assertThat(out[1]).isNull();
    }

    @Test
    void allDigitsInputNotMistakenAsPhoneOnly() {
        // 拆完地址为空 → 原样归地址(供应商给的就不是混排)
        String[] out = JumeiCompanyRegistryAdapter.splitAddressPhone("0571-88888888");
        assertThat(out[0]).isEqualTo("0571-88888888");
        assertThat(out[1]).isNull();
    }

    @Test
    void nullAddressYieldsNulls() {
        String[] out = JumeiCompanyRegistryAdapter.splitAddressPhone(null);
        assertThat(out[0]).isNull();
        assertThat(out[1]).isNull();
    }

    @Test
    void toRecordCleansAndSplits() {
        var record = JumeiCompanyRegistryAdapter.toRecord(new JumeiData(
                " 腾讯科技（深圳）有限公司 ", "9144030071526726XG",
                "深圳市南山区腾讯大厦35层0755-86013388", "  ", "817281823910001"));
        assertThat(record.name()).isEqualTo("腾讯科技（深圳）有限公司");
        assertThat(record.taxNo()).isEqualTo("9144030071526726XG");
        assertThat(record.address()).isEqualTo("深圳市南山区腾讯大厦35层");
        assertThat(record.phone()).isEqualTo("0755-86013388");
        assertThat(record.bankName()).isNull(); // 空白清洗成 null
        assertThat(record.bankAccount()).isEqualTo("817281823910001");
    }

    @Test
    void blankAppcodeDisablesFeature() {
        var adapter = new JumeiCompanyRegistryAdapter("https://example.invalid", " ");
        assertThat(adapter.cached("任意公司")).isEmpty();
        assertThatThrownBy(() -> adapter.lookup("任意公司"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("核验");
    }
}
