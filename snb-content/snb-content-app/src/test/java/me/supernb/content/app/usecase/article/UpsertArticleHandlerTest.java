package me.supernb.content.app.usecase.article;

import me.supernb.content.app.usecase.article.command.UpsertArticleCommand;
import me.supernb.content.app.usecase.article.dto.UpsertResult;
import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.port.repository.ArticleRepository;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// UpsertArticleHandler 纯单测：分类守卫 + tags 归一化 + id String 化。
@ExtendWith(MockitoExtension.class)
class UpsertArticleHandlerTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    UpsertArticleHandler handler;

    UpsertArticleCommand command(String slug, String category, List<String> tags) {
        return new UpsertArticleCommand(slug, "article", "标题", "s", null, category, tags,
                "<p>hi</p>", null, null, null, Instant.parse("2026-07-10T00:00:00Z"), false);
    }

    @Test
    void rejectsUnknownCategory() {
        when(categoryRepository.exists("nope")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(command("hello", "nope", List.of())))
                .isInstanceOf(ContentException.class);
        verifyNoInteractions(articleRepository);
    }

    @Test
    void normalizesNullTagsAndStringifiesId() {
        when(categoryRepository.exists("tutorials")).thenReturn(true);
        when(articleRepository.upsert(any())).thenReturn(new ArticleRepository.UpsertOutcome(42L, true));

        UpsertResult result = handler.handle(command("hello", "tutorials", null));

        assertThat(result).isEqualTo(new UpsertResult("42", true));
        verify(articleRepository).upsert(new ArticleRepository.ArticleData(
                "hello", "article", "标题", "s", null, "tutorials", List.of(),
                "<p>hi</p>", null, null, null, Instant.parse("2026-07-10T00:00:00Z"), false));
    }
}
