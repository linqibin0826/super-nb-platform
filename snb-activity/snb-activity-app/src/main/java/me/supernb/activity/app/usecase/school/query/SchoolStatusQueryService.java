package me.supernb.activity.app.usecase.school.query;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.SchoolStatusView;
import me.supernb.activity.app.usecase.school.SchoolStatusView.CardBlock;
import me.supernb.activity.app.usecase.school.SchoolStatusView.FirstChargeBlock;
import me.supernb.activity.app.usecase.school.SchoolStatusView.InviteBlock;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.springframework.stereotype.Service;

/// 包机活动状态查询:双线资格 + 领取态合成视图。领取命令与本查询共用这一份判定,
/// 避免两处口径漂移(thursday 同款纪律);时间标签服务端出成品,前端照抄不换算。
///
/// 资格事件边界钉死 [start, end)(首充 completedAt、被邀计数都以 end 为界),
/// 宽限期 [end, claimDeadline) 只放领取不产生新资格。
@Service
public class SchoolStatusQueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter END_LABEL = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final SchoolSeasonProperties props;
    private final SchoolReadPort readPort;
    private final SchoolClaimPort claimPort;
    private final SchoolCardPort cardPort;

    /// 构造:注入活动配置、只读端口(首充/计数)、首充领取台账、邀请卡端口。
    public SchoolStatusQueryService(SchoolSeasonProperties props, SchoolReadPort readPort,
            SchoolClaimPort claimPort, SchoolCardPort cardPort) {
        this.props = props;
        this.readPort = readPort;
        this.claimPort = claimPort;
        this.cardPort = cardPort;
    }

    /// 本人当前活动视图。
    public SchoolStatusView view(long userId) {
        return view(userId, Instant.now());
    }

    /// 同上,`now` 显式传入以便单测钉住「窗口内/宽限期/收官后」分支。
    public SchoolStatusView view(long userId, Instant now) {
        if (!props.configured() || now.isBefore(props.start()) || !now.isBefore(props.claimDeadline())) {
            return SchoolStatusView.closed();
        }
        return new SchoolStatusView(true, endsAtLabel(), firstChargeBlock(userId), inviteBlock(userId));
    }

    private String endsAtLabel() {
        return props.end().atZone(ZONE).format(END_LABEL);
    }

    private FirstChargeBlock firstChargeBlock(long userId) {
        Optional<SchoolReadPort.FirstCharge> first = readPort.firstCharge(userId);
        if (first.isEmpty()) {
            return new FirstChargeBlock(false, false, 0, "", SchoolStatusView.STATUS_NONE);
        }
        SchoolReadPort.FirstCharge fc = first.get();
        boolean inWindow = !fc.completedAt().isBefore(props.start()) && fc.completedAt().isBefore(props.end());
        int tierCard = inWindow ? tierCardFor(fc) : 0;
        String status = tierCard == 0 ? SchoolStatusView.STATUS_NONE
                : claimStatus(userId, tierCard);
        return new FirstChargeBlock(true, inWindow, tierCard, fc.amountCny().toPlainString(), status);
    }

    /// 按门槛降序取满足的最高档;不足最低档(¥30)返回 0。
    private static int tierCardFor(SchoolReadPort.FirstCharge fc) {
        for (int i = 0; i < SchoolSeasonProperties.FIRST_CHARGE_TIERS_CNY.length; i++) {
            if (fc.amountCny().compareTo(
                    java.math.BigDecimal.valueOf(SchoolSeasonProperties.FIRST_CHARGE_TIERS_CNY[i])) >= 0) {
                return SchoolSeasonProperties.FIRST_CHARGE_CARDS[i];
            }
        }
        return 0;
    }

    /// 邀请卡块(v2):已领档从 school_card 读,应得档/重置获得侧从合格人数现推。
    private InviteBlock inviteBlock(long userId) {
        int count = readPort.qualifiedInviteeCount(userId, props.start(), props.end(),
                SchoolSeasonProperties.INVITEE_MIN_CNY);
        Optional<SchoolCardRecord> card = cardPort.find(userId);
        int tier = card.map(SchoolCardRecord::tier).orElse(0);
        int deserved = SchoolSeasonProperties.deservedTier(count);
        int used = card.map(SchoolCardRecord::resetsUsed).orElse(0);
        int available = Math.max(0, SchoolSeasonProperties.resetsEarned(count) - used);
        CardBlock cb = new CardBlock(tier, tierName(tier), tierAmount(tier),
                deserved, tierName(deserved), tierAmount(deserved),
                available, used);
        return new InviteBlock(count, cb, count >= SchoolSeasonProperties.KFC_TIER);
    }

    private static String tierName(int tier) {
        return tier <= 0 ? "" : SchoolSeasonProperties.CARD_NAMES[tier - 1];
    }

    private static int tierAmount(int tier) {
        return tier <= 0 ? 0 : SchoolSeasonProperties.CARD_AMOUNTS[tier - 1];
    }

    /// 首充领取态映射:判重唯一真源=领取表(绝不用订阅 notes 匹配)。
    private String claimStatus(long userId, int tier) {
        return claimPort.find(userId, SchoolClaimRecord.KIND_FIRST_CHARGE, tier)
                .map(r -> switch (r.grantStatus()) {
                    case SchoolClaimRecord.STATUS_SUCCESS -> SchoolStatusView.STATUS_CLAIMED;
                    case SchoolClaimRecord.STATUS_FAILED -> SchoolStatusView.STATUS_FAILED;
                    default -> SchoolStatusView.STATUS_PENDING;
                })
                .orElse(SchoolStatusView.STATUS_CLAIMABLE);
    }
}
