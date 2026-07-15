package me.supernb.invoice.app.usecase.profile.query;

import java.util.List;
import me.supernb.invoice.domain.model.read.ProfileView;
import me.supernb.invoice.domain.port.read.InvoiceProfileReadPort;
import org.springframework.stereotype.Service;

/// 抬头查询服务:Controller 直接注入,读视图原样透出。
@Service
public class ProfileQueryService {

    private final InvoiceProfileReadPort readPort;

    /// 构造:注入抬头只读端口。
    public ProfileQueryService(InvoiceProfileReadPort readPort) {
        this.readPort = readPort;
    }

    /// 我的全部抬头。
    public List<ProfileView> listByUser(long userId) {
        return readPort.listByUser(userId);
    }
}
