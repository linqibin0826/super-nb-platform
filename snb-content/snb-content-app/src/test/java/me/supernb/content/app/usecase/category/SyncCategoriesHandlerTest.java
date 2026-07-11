package me.supernb.content.app.usecase.category;

import me.supernb.content.app.usecase.category.command.SyncCategoriesCommand;
import me.supernb.content.app.usecase.category.dto.SyncResult;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/// SyncCategoriesHandler 纯单测：委托 + 结果映射。
@ExtendWith(MockitoExtension.class)
class SyncCategoriesHandlerTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    SyncCategoriesHandler handler;

    @Test
    void delegatesAndReports() {
        when(categoryRepository.sync(anyList())).thenReturn(new CategoryRepository.SyncOutcome(3, 1));

        SyncResult result = handler.handle(new SyncCategoriesCommand(List.of(
                new CategoryRepository.CategoryData("tutorials", "教程", 1))));

        assertThat(result).isEqualTo(new SyncResult(3, 1));
    }
}
