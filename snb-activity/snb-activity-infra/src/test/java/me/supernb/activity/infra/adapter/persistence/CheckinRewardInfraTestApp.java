package me.supernb.activity.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// 补给发放台账 infra 最小装配:只挂被测 CheckinRewardAdapter。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(CheckinRewardAdapter.class)
class CheckinRewardInfraTestApp {
}
