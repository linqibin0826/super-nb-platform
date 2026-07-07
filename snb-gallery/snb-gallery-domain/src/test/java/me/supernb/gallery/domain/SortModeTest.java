package me.supernb.gallery.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SortModeTest {

    @Test
    void parsesKnownValues() {
        assertThat(SortMode.from("newest")).isEqualTo(SortMode.NEWEST);
        assertThat(SortMode.from("likes")).isEqualTo(SortMode.LIKES);
        assertThat(SortMode.from("favorites")).isEqualTo(SortMode.FAVORITES);
        assertThat(SortMode.from("featured")).isEqualTo(SortMode.FEATURED);
    }

    @Test
    void unknownAndNullFallBackToFeatured() {
        assertThat(SortMode.from("bogus")).isEqualTo(SortMode.FEATURED);
        assertThat(SortMode.from(null)).isEqualTo(SortMode.FEATURED);
    }
}
