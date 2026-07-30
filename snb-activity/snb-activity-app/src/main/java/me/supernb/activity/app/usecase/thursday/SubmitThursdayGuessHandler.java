package me.supernb.activity.app.usecase.thursday;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.activity.app.usecase.thursday.command.SubmitThursdayGuessCommand;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort;
import org.springframework.stereotype.Service;

/// 猜桶竞猜提交编排:范围校验 → 场次/门槛/封猜三道闸(服务端重算) → 落库(一人一场一猜)。
///
/// 不合法一律**静默返回当前视图**而不是抛错:猜桶是个玩票环节,门槛外/已封猜的人
/// 点一下不该看见红色报错;前端照视图渲染即可(open=false 就不给输入框)。
/// 与发卡不同——那个是发钱,失败必须炸。
@Service
public class SubmitThursdayGuessHandler implements CommandHandler<SubmitThursdayGuessCommand, ThursdayGuessView> {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ThursdayProperties props;
    private final ThursdayBucketQueryService query;
    private final ThursdayGuessPort guessPort;

    /// 构造:注入配置、判定用的查询服务与猜测存取端口。
    public SubmitThursdayGuessHandler(ThursdayProperties props, ThursdayBucketQueryService query,
            ThursdayGuessPort guessPort) {
        this.props = props;
        this.query = query;
        this.guessPort = guessPort;
    }

    @Override
    public ThursdayGuessView handle(SubmitThursdayGuessCommand command) {
        Instant now = Instant.now();
        int guess = command.guess();
        // 范围必须服务端校验:客户端传 -1 或 9999 会污染结算(|guess-answer| 直接把它顶成赢家)。
        boolean sane = guess >= 0 && guess <= props.bucketLimit();
        if (sane && query.guessAcceptable(command.userId(), now)) {
            guessPort.submitOnce(LocalDate.now(ZONE), command.userId(), guess);
        }
        return query.guessView(command.userId(), now);
    }
}
