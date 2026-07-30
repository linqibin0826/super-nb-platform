package me.supernb.activity.app.usecase.thursday;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import me.supernb.activity.app.usecase.thursday.command.SubmitThursdayGuessCommand;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort;
import org.junit.jupiter.api.Test;

/// 提交闸门:越界不收、不够门槛/已封猜不收、合法才落库。
class SubmitThursdayGuessHandlerTest {

    private final ThursdayBucketQueryService query = mock(ThursdayBucketQueryService.class);
    private final ThursdayGuessPort guessPort = mock(ThursdayGuessPort.class);

    private SubmitThursdayGuessHandler handler() {
        return new SubmitThursdayGuessHandler(
                new ThursdayProperties("", new BigDecimal("50"), 50, 1, "opening-fk", 3, "salt", "22:00",
                        "20:00", new BigDecimal("30")),
                query, guessPort);
    }

    private void acceptable(boolean ok) {
        when(query.guessAcceptable(anyLong(), any())).thenReturn(ok);
    }

    /// 🚨 范围必须服务端校验:客户端塞 9999 或 -1 会直接把自己顶成赢家
    /// (|guess-answer| 排序下,离谱值反而独占一个极值)。
    @Test
    void outOfRangeGuessIsNotStored() {
        acceptable(true);
        handler().handle(new SubmitThursdayGuessCommand(7, 9999));
        handler().handle(new SubmitThursdayGuessCommand(7, -1));
        handler().handle(new SubmitThursdayGuessCommand(7, 51));
        verify(guessPort, never()).submitOnce(any(), anyLong(), anyInt());
    }

    @Test
    void boundaryValuesAreAccepted() {
        acceptable(true);
        handler().handle(new SubmitThursdayGuessCommand(7, 0));
        handler().handle(new SubmitThursdayGuessCommand(7, 50));
        verify(guessPort).submitOnce(any(LocalDate.class), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(0));
        verify(guessPort).submitOnce(any(LocalDate.class), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(50));
    }

    /// 门槛外 / 已封猜 / 非场次日:一律不落库,也不抛错(玩票环节不给红色报错)。
    @Test
    void unacceptableSubmissionIsSilentlyDropped() {
        acceptable(false);
        handler().handle(new SubmitThursdayGuessCommand(7, 20));
        verify(guessPort, never()).submitOnce(any(), anyLong(), anyInt());
    }

    @Test
    void acceptableSubmissionIsStored() {
        acceptable(true);
        handler().handle(new SubmitThursdayGuessCommand(7, 20));
        verify(guessPort).submitOnce(any(LocalDate.class), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(20));
    }
}
