package me.supernb.guide.app.usecase.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.supernb.guide.domain.exception.GuideException;
import me.supernb.guide.domain.model.GuideKey;
import me.supernb.guide.domain.port.repository.GuideAckRepository;
import org.junit.jupiter.api.Test;

/// 已读用例:格式把门 → 幂等短路 → 单用户上限封顶(防脚本刷表膨胀)。
class AckGuideHandlerTest {

    private final GuideAckRepository acks = mock(GuideAckRepository.class);
    private final AckGuideHandler handler = new AckGuideHandler(acks);

    @Test
    void newKeyUnderLimitGetsPersisted() {
        when(acks.ackedKeys(1L)).thenReturn(Set.of());
        handler.handle(new AckGuideCommand(1L, "invoice.intro.v1"));
        verify(acks).ack(1L, "invoice.intro.v1");
    }

    @Test
    void alreadyAckedShortCircuitsWithoutWriting() {
        when(acks.ackedKeys(1L)).thenReturn(Set.of("invoice.intro.v1"));
        handler.handle(new AckGuideCommand(1L, "invoice.intro.v1"));
        verify(acks, never()).ack(anyLong(), anyString());
    }

    @Test
    void newKeyAtLimitIsRejected() {
        Set<String> full = IntStream.range(0, GuideKey.MAX_PER_USER)
                .mapToObj(i -> "k" + i)
                .collect(Collectors.toSet());
        when(acks.ackedKeys(1L)).thenReturn(full);
        assertThatThrownBy(() -> handler.handle(new AckGuideCommand(1L, "brand.new.key")))
                .isInstanceOf(GuideException.class);
        verify(acks, never()).ack(anyLong(), anyString());
    }

    @Test
    void alreadyAckedAtLimitStillShortCircuits() {
        // 已达上限但重复 ack 已存在的 key:幂等成功,不受上限拦截
        Set<String> full = IntStream.range(0, GuideKey.MAX_PER_USER)
                .mapToObj(i -> "k" + i)
                .collect(Collectors.toSet());
        when(acks.ackedKeys(1L)).thenReturn(full);
        handler.handle(new AckGuideCommand(1L, "k0"));
        verify(acks, never()).ack(anyLong(), anyString());
    }

    @Test
    void invalidKeyRejectedBeforeLimitCheck() {
        assertThatThrownBy(() -> handler.handle(new AckGuideCommand(1L, "BAD KEY!")))
                .isInstanceOf(GuideException.class);
        verify(acks, never()).ackedKeys(anyLong());
    }
}
