package me.supernb.content.app.usecase.article;

import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.port.read.ContentReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/// ArticleQueries 纯单测：404 语义收在查询服务内（Controller 不写业务判断）。
@ExtendWith(MockitoExtension.class)
class ArticleQueriesTest {

    @Mock
    ContentReadPort readPort;

    @InjectMocks
    ArticleQueries queries;

    @Test
    void detailOrThrowMaps404() {
        when(readPort.findVisibleBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queries.detailOrThrow("nope")).isInstanceOf(ContentException.class);
    }
}
