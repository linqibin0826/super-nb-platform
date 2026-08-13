package me.supernb.activity.app.usecase.school.query;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.SchoolStatusView;
import me.supernb.activity.app.usecase.school.SchoolStatusView.FirstChargeBlock;
import me.supernb.activity.app.usecase.school.SchoolStatusView.InviteBlock;
import me.supernb.activity.app.usecase.school.SchoolStatusView.Milestone;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.springframework.stereotype.Service;

/// 开学季状态查询:双线资格 + 领取态合成视图。领取命令与本查询共用这一份判定,
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

    /// 构造:注入活动配置、只读端口(首充/计数)、领取台账端口。
    public SchoolStatusQueryService(SchoolSeasonProperties props, SchoolReadPort readPort,
            SchoolClaimPort claimPort) {
        this.props = props;
        this.readPort = readPort;
        this.claimPort = claimPort;
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
                : claimStatus(userId, SchoolClaimRecord.KIND_FIRST_CHARGE, tierCard);
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

    private InviteBlock inviteBlock(long userId) {
        int raw = readPort.qualifiedInviteeCount(userId, props.start(), props.end(),
                SchoolSeasonProperties.INVITEE_MIN_CNY);
        int count = Math.min(raw, SchoolSeasonProperties.INVITE_CAP);
        List<Milestone> milestones = new ArrayList<>();
        for (int i = 0; i < SchoolSeasonProperties.MILESTONE_TIERS.length; i++) {
            int tier = SchoolSeasonProperties.MILESTONE_TIERS[i];
            int card = SchoolSeasonProperties.MILESTONE_CARDS[i];
            boolean unlocked = count >= tier;
            String status = unlocked
                    ? claimStatus(userId, SchoolClaimRecord.KIND_MILESTONE, tier)
                    : SchoolStatusView.STATUS_NONE;
            milestones.add(new Milestone(tier, card, unlocked, status));
        }
        return new InviteBlock(count, List.copyOf(milestones), count >= SchoolSeasonProperties.KFC_TIER);
    }

    /// 领取态映射:判重唯一真源=领取表(绝不用订阅 notes 匹配)。
    private String claimStatus(long userId, String kind, int tier) {
        return claimPort.find(userId, kind, tier)
                .map(r -> switch (r.grantStatus()) {
                    case SchoolClaimRecord.STATUS_SUCCESS -> SchoolStatusView.STATUS_CLAIMED;
                    case SchoolClaimRecord.STATUS_FAILED -> SchoolStatusView.STATUS_FAILED;
                    default -> SchoolStatusView.STATUS_PENDING;
                })
                .orElse(SchoolStatusView.STATUS_CLAIMABLE);
    }
}
