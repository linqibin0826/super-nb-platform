package me.supernb.invoice.domain.port.read;

import java.util.List;
import me.supernb.invoice.domain.model.read.ProfileView;

/// 抬头只读投影。
public interface InvoiceProfileReadPort {

    /// 该用户全部抬头(创建时间正序)。
    List<ProfileView> listByUser(long userId);
}
