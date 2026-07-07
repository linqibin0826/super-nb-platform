---
paths: snb-*/snb-*-domain/**/src/test/**/*.java, snb-*/snb-*-app/**/src/test/**/*.java, snb-common/**/src/test/**/*.java
---

# 单元测试规范（domain / app / snb-common）

## 适用范围

- domain：纯单测，无 mock（规则计算直接断言，覆盖全部分支）
- app：单测 + Mockito mock 端口（验编排、异常传播、降级语义）
- snb-common：纯单测（如令牌桶的时间推进用注入时钟，不真 sleep）

## 硬性要求

1. **零 Spring**：不起上下文、不用 `@SpringBootTest`/`@MockitoBean`——被测对象 `new` 出来，端口用 `mock(XxxPort.class)` 构造注入
2. **不碰库、不碰网络、不真 sleep**；`@Timeout` ≤ 2s
3. 领域异常断言到**具体类型**（`isInstanceOf(NoDrawsLeftException.class)`），不断言消息文案

## app 用例测试样板（真实代码 `PerformDrawUseCaseTest`）

```java
class PerformDrawUseCaseTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final PerformDrawUseCase useCase = new PerformDrawUseCase(campaignPort, drawPort);

    @Test
    void delegatesToDrawPort() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenReturn(DrawResult.prize(new BigDecimal("20"), "CODE1"));

        DrawResult r = useCase.draw(7);

        assertThat(r.redeemCode()).isEqualTo("CODE1");
    }

    @Test
    void propagatesNoDrawsLeft() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenThrow(new NoDrawsLeftException());
        assertThatThrownBy(() -> useCase.draw(7)).isInstanceOf(NoDrawsLeftException.class);
    }
}
```

要点：字段初始化夹具（无 `@BeforeEach` 仪式）、方法名即规格、异常传播是 app 层的显式契约（不吞不包装，见 tech/error-handling.md）。

## 必须覆盖的用例语义

- happy path + 每个业务异常分支
- **优雅降级**：「无进行中活动 → 空集合」这类语义是契约，单独成测（样板 `GracefulDegradationTest`：一个测试把全部公开查询用例的降级钉死）
- 边界：资格金额档位边界、次数用尽、幂等重放
