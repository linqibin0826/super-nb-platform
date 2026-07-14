package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;

import me.supernb.activity.domain.port.read.GalleryAchievementSignalPort;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 同库跨 schema 原生 SQL:generation 只计 done 状态,点赞+收藏两表相加。
@Testcontainers
class GalleryAchievementSignalAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static GalleryAchievementSignalAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA gallery");
        jdbc.execute("CREATE TABLE gallery.generation (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "status TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE gallery.prompt_like (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE gallery.prompt_favorite (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL)");
        adapter = new GalleryAchievementSignalAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE gallery.generation, gallery.prompt_like, gallery.prompt_favorite");
    }

    @Test
    void generationDoneCountsOnlyCountsDoneStatus() {
        jdbc.update("INSERT INTO gallery.generation (id, user_id, status) VALUES (1, 42, 'done')");
        jdbc.update("INSERT INTO gallery.generation (id, user_id, status) VALUES (2, 42, 'error')");
        jdbc.update("INSERT INTO gallery.generation (id, user_id, status) VALUES (3, 42, 'done')");
        assertThat(adapter.generationDoneCounts())
                .containsExactly(new GalleryAchievementSignalPort.UserCount(42L, 2L));
    }

    @Test
    void likeAndFavoriteCountsSumsBothTables() {
        jdbc.update("INSERT INTO gallery.prompt_like (id, user_id) VALUES (1, 7)");
        jdbc.update("INSERT INTO gallery.prompt_favorite (id, user_id) VALUES (2, 7)");
        jdbc.update("INSERT INTO gallery.prompt_favorite (id, user_id) VALUES (3, 7)");
        assertThat(adapter.likeAndFavoriteCounts())
                .containsExactly(new GalleryAchievementSignalPort.UserCount(7L, 3L));
    }
}
