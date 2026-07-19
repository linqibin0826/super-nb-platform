package me.supernb.activity.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import me.supernb.activity.adapter.rest.request.AddRafflePrizeRequest;
import me.supernb.activity.adapter.rest.request.CreateRaffleCampaignRequest;
import me.supernb.activity.adapter.rest.request.GenerateAlipayCodeRequest;
import me.supernb.activity.adapter.rest.request.GenerateRedeemCodeForPrizeRequest;
import me.supernb.activity.adapter.rest.request.GenerateRedeemCodesRequest;
import me.supernb.activity.adapter.rest.request.UpdateRaffleCampaignRequest;
import me.supernb.activity.adapter.rest.request.UpdateRafflePrizeRequest;
import me.supernb.activity.adapter.rest.response.RaffleAdminCampaignDetail;
import me.supernb.activity.adapter.rest.response.RaffleAdminCampaignSummary;
import me.supernb.activity.adapter.rest.response.RaffleAdminPrize;
import me.supernb.activity.app.usecase.raffle.command.AddRafflePrizeCommand;
import me.supernb.activity.app.usecase.raffle.command.CancelRaffleCampaignCommand;
import me.supernb.activity.app.usecase.raffle.command.CreateRaffleCampaignCommand;
import me.supernb.activity.app.usecase.raffle.command.DeleteRafflePrizeCommand;
import me.supernb.activity.app.usecase.raffle.command.GenerateRaffleAlipayCodeCommand;
import me.supernb.activity.app.usecase.raffle.command.GenerateRaffleRedeemCodeForPrizeCommand;
import me.supernb.activity.app.usecase.raffle.command.GenerateRaffleRedeemCodesCommand;
import me.supernb.activity.app.usecase.raffle.command.UpdateRaffleCampaignCommand;
import me.supernb.activity.app.usecase.raffle.command.UpdateRafflePrizeCommand;
import me.supernb.activity.domain.exception.RaffleAdminForbiddenException;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.exception.RafflePrizeNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 抽奖管理端点:新建/编辑/作废期(克隆由前端用 detail 拼装,不设专属后端端点),
/// 增删改/生成奖品。全部要求 role=admin,校验模式照抄 InvoiceAdminController——
/// 每个方法首行 requireAdmin(user)。
@RestController
@RequestMapping("/activity/v1/admin/raffle")
public class RaffleAdminController {

    private final CommandBus commandBus;
    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public RaffleAdminController(CommandBus commandBus, RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.commandBus = commandBus;
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    private static void requireAdmin(UserProfile user) {
        if (!user.isAdmin()) {
            throw new RaffleAdminForbiddenException();
        }
    }

    @GetMapping("/campaigns")
    public List<RaffleAdminCampaignSummary> list(@CurrentUser UserProfile user) {
        requireAdmin(user);
        return campaignPort.listAll().stream()
                .map(c -> RaffleAdminCampaignSummary.of(c, prizePort.byCampaign(c.id()).size()))
                .toList();
    }

    @GetMapping("/campaigns/{id}")
    public RaffleAdminCampaignDetail detail(@CurrentUser UserProfile user, @PathVariable long id) {
        requireAdmin(user);
        return toDetail(id);
    }

    @PostMapping("/campaigns")
    public RaffleAdminCampaignDetail create(@CurrentUser UserProfile user,
            @RequestBody CreateRaffleCampaignRequest body) {
        requireAdmin(user);
        List<CreateRaffleCampaignCommand.PrizeSkeleton> prizes = body.prizes() == null ? List.of()
                : body.prizes().stream()
                        .map(p -> new CreateRaffleCampaignCommand.PrizeSkeleton(
                                p.tier(), p.displayName(), p.kind(), p.sortOrder()))
                        .toList();
        long id = commandBus.handle(new CreateRaffleCampaignCommand(body.name(), body.entryOpenAt(),
                body.entryCloseAt(), body.drawAt(), parseGateType(body.gateType()), body.gateAmount(),
                body.gateFrom(), body.minAccountAgeDays(), parseWeightMode(body.weightMode()), prizes));
        return toDetail(id);
    }

    @PutMapping("/campaigns/{id}")
    public RaffleAdminCampaignDetail update(@CurrentUser UserProfile user, @PathVariable long id,
            @RequestBody UpdateRaffleCampaignRequest body) {
        requireAdmin(user);
        commandBus.handle(new UpdateRaffleCampaignCommand(id, body.name(), body.entryOpenAt(),
                body.entryCloseAt(), body.drawAt(), parseGateType(body.gateType()), body.gateAmount(),
                body.gateFrom(), body.minAccountAgeDays(), parseWeightMode(body.weightMode())));
        return toDetail(id);
    }

    @PostMapping("/campaigns/{id}/cancel")
    public void cancel(@CurrentUser UserProfile user, @PathVariable long id) {
        requireAdmin(user);
        commandBus.handle(new CancelRaffleCampaignCommand(id));
    }

    @PostMapping("/campaigns/{id}/prizes")
    public RaffleAdminPrize addPrize(@CurrentUser UserProfile user, @PathVariable long id,
            @RequestBody AddRafflePrizeRequest body) {
        requireAdmin(user);
        long prizeId = commandBus.handle(new AddRafflePrizeCommand(id, body.tier(), body.displayName(),
                body.kind(), body.payload(), body.sortOrder()));
        return toPrize(id, prizeId);
    }

    @PutMapping("/campaigns/{id}/prizes/{prizeId}")
    public RaffleAdminPrize updatePrize(@CurrentUser UserProfile user, @PathVariable long id,
            @PathVariable long prizeId, @RequestBody UpdateRafflePrizeRequest body) {
        requireAdmin(user);
        commandBus.handle(new UpdateRafflePrizeCommand(id, prizeId, body.tier(), body.displayName(),
                body.kind(), body.payload(), body.sortOrder()));
        return toPrize(id, prizeId);
    }

    @DeleteMapping("/campaigns/{id}/prizes/{prizeId}")
    public void deletePrize(@CurrentUser UserProfile user, @PathVariable long id, @PathVariable long prizeId) {
        requireAdmin(user);
        commandBus.handle(new DeleteRafflePrizeCommand(id, prizeId));
    }

    @PostMapping("/campaigns/{id}/prizes/generate-redeem-codes")
    public List<RaffleAdminPrize> generateRedeemCodes(@CurrentUser UserProfile user, @PathVariable long id,
            @RequestBody GenerateRedeemCodesRequest body) {
        requireAdmin(user);
        List<Long> prizeIds = commandBus.handle(new GenerateRaffleRedeemCodesCommand(id, body.tier(),
                body.displayName(), body.groupId(), body.validityDays(), body.count(), body.sortOrderStart()));
        return prizeIds.stream().map(pid -> toPrize(id, pid)).toList();
    }

    /// 对指定空壳兑换码行生成 1 张码就地回填(克隆骨架逐行点亮)。
    @PostMapping("/campaigns/{id}/prizes/{prizeId}/generate-redeem-code")
    public RaffleAdminPrize generateRedeemCodeForPrize(@CurrentUser UserProfile user, @PathVariable long id,
            @PathVariable long prizeId, @RequestBody GenerateRedeemCodeForPrizeRequest body) {
        requireAdmin(user);
        long filled = commandBus.handle(new GenerateRaffleRedeemCodeForPrizeCommand(id, prizeId,
                body.groupId(), body.validityDays()));
        return toPrize(id, filled);
    }

    @PostMapping("/campaigns/{id}/prizes/generate-alipay-code")
    public RaffleAdminPrize generateAlipayCode(@CurrentUser UserProfile user, @PathVariable long id,
            @RequestBody GenerateAlipayCodeRequest body) {
        requireAdmin(user);
        long prizeId = commandBus.handle(new GenerateRaffleAlipayCodeCommand(id, parsePrizeId(body.prizeId()),
                body.tier(), body.displayName(), body.sortOrder()));
        return toPrize(id, prizeId);
    }

    private RaffleAdminCampaignDetail toDetail(long campaignId) {
        RaffleCampaign c = campaignPort.byId(campaignId).orElseThrow(RaffleNotFoundException::new);
        return RaffleAdminCampaignDetail.of(c, prizePort.byCampaign(campaignId));
    }

    private RaffleAdminPrize toPrize(long campaignId, long prizeId) {
        RafflePrize p = prizePort.byCampaign(campaignId).stream()
                .filter(x -> x.id() == prizeId)
                .findFirst()
                .orElseThrow(RafflePrizeNotFoundException::new);
        return RaffleAdminPrize.of(p);
    }

    private static GateType parseGateType(String raw) {
        try {
            return GateType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RaffleAdminValidationException("门槛类型必须是 RECHARGE 或 SPEND");
        }
    }

    private static WeightMode parseWeightMode(String raw) {
        try {
            return WeightMode.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RaffleAdminValidationException("权重模式必须是 EQUAL 或 WEIGHTED");
        }
    }

    /// prizeId 前端传 String(id 一律 string 契约),这里手动转 Long;null/空串=新建路径。
    private static Long parsePrizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new RaffleAdminValidationException("prizeId 格式非法");
        }
    }
}
