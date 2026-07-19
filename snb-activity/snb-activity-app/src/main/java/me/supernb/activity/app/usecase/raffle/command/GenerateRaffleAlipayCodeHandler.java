package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.security.SecureRandom;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class GenerateRaffleAlipayCodeHandler implements CommandHandler<GenerateRaffleAlipayCodeCommand, Long> {

    // 排除易混字符(0/O/1/I),照 raffle runbook 里"从 $RANDOM 改用 python secrets"的教训,
    // 这里直接用 SecureRandom(JDK 自带密码学安全随机源)。同字符集组合数 32^6 ≈ 10.7 亿,
    // 一期几十件奖品的碰撞概率可忽略,不做数据库唯一性校验。
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public GenerateRaffleAlipayCodeHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    @Override
    public Long handle(GenerateRaffleAlipayCodeCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        String passphrase = generatePassphrase();
        if (command.prizeId() != null) {
            prizePort.updatePayload(command.prizeId(), passphrase);
            return command.prizeId();
        }
        return prizePort.create(command.campaignId(), command.tier(), command.displayName(),
                "ALIPAY_CODE", passphrase, command.sortOrder());
    }

    private static String generatePassphrase() {
        StringBuilder sb = new StringBuilder("SuperNB");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
