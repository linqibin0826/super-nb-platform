package me.supernb.activity.domain.model.achievement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AchievementStatusTransitionTest {

    @Test
    void draftCanMoveToActive() {
        assertThat(AchievementStatusTransition.isValidForward("draft", "active")).isTrue();
    }

    @Test
    void activeCanMoveToRetired() {
        assertThat(AchievementStatusTransition.isValidForward("active", "retired")).isTrue();
    }

    @Test
    void sameStatusIsIdempotentlyValid() {
        assertThat(AchievementStatusTransition.isValidForward("active", "active")).isTrue();
    }

    @Test
    void retiredHasNoWayBack() {
        assertThat(AchievementStatusTransition.isValidForward("retired", "active")).isFalse();
        assertThat(AchievementStatusTransition.isValidForward("retired", "draft")).isFalse();
    }

    @Test
    void activeCannotGoBackToDraft() {
        assertThat(AchievementStatusTransition.isValidForward("active", "draft")).isFalse();
    }

    @Test
    void draftCannotSkipToRetired() {
        assertThat(AchievementStatusTransition.isValidForward("draft", "retired")).isFalse();
    }
}
