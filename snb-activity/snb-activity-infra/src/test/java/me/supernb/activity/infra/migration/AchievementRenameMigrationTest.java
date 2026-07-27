package me.supernb.activity.infra.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/// V15 网吧化重命名迁移的**静态**完整性校验（不连库、不需要 Docker）。
///
/// 为什么值得单独写：`UPDATE ... WHERE code = 'xxx'` 拼错一个 code 会**影响 0 行且不报错**——
/// 那条成就静默保留旧名字，要等用户在成就墙上看见才发现。类目同理。
/// 这类「静默漏改」是纯 SQL 迁移最容易翻车的地方，编译器和 Flyway 都拦不住。
class AchievementRenameMigrationTest {

    private static final Pattern V9_CODE = Pattern.compile("^\\(9\\d{5}, '([a-z0-9_]+)'", Pattern.MULTILINE);
    private static final Pattern V9_CATEGORY =
            Pattern.compile("^\\(9\\d{5}, '[a-z0-9_]+', (?:NULL|'[^']*'), (?:NULL|\\d+), '([^']+)'",
                    Pattern.MULTILINE);
    private static final Pattern V15_CODE = Pattern.compile("WHERE code = '([a-z0-9_]+)'");
    private static final Pattern V15_CATEGORY = Pattern.compile("WHERE category = '([^']+)'");

    private static String read(String name) throws IOException {
        try (InputStream in = AchievementRenameMigrationTest.class.getClassLoader()
                .getResourceAsStream("db/migration/activity/" + name)) {
            assertThat(in).as("找不到迁移文件 %s", name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> matches(Pattern p, String src) {
        Matcher m = p.matcher(src);
        return m.results().map(r -> r.group(1)).collect(Collectors.toList());
    }

    @Test
    void v15CoversEveryCodeDefinedInV9() throws IOException {
        Set<String> v9Codes = new LinkedHashSet<>(matches(V9_CODE, read("V9__achievement_baseline.sql")));
        List<String> v15Codes = matches(V15_CODE, read("V15__achievement_wangba_rename.sql"));

        assertThat(v9Codes).as("V9 应定义 42 条成就").hasSize(42);
        // 幽灵 code：V15 写了但 V9 没有 → UPDATE 影响 0 行，静默无效
        assertThat(v15Codes).as("V15 不得引用 V9 里不存在的 code").allSatisfy(c -> assertThat(v9Codes).contains(c));
        // 漏改：V9 有但 V15 没覆盖 → 那条成就保留旧名字
        assertThat(new LinkedHashSet<>(v15Codes)).as("V15 必须覆盖 V9 的全部 code").isEqualTo(v9Codes);
    }

    @Test
    void v15HasNoDuplicateCodeUpdates() throws IOException {
        List<String> v15Codes = matches(V15_CODE, read("V15__achievement_wangba_rename.sql"));
        assertThat(v15Codes).as("同一个 code 被改两次说明有笔误，后一条会覆盖前一条")
                .doesNotHaveDuplicates();
    }

    @Test
    void v15CoversEveryCategoryUsedInV9() throws IOException {
        Set<String> v9Cats = new LinkedHashSet<>(matches(V9_CATEGORY, read("V9__achievement_baseline.sql")));
        Set<String> v15Cats = new LinkedHashSet<>(matches(V15_CATEGORY, read("V15__achievement_wangba_rename.sql")));

        assertThat(v9Cats).as("V9 实际使用 8 个类目").hasSize(8);
        assertThat(v15Cats).as("V15 必须覆盖 V9 用到的每一个类目").containsAll(v9Cats);
    }

    /// 🚨 V9/V10 是已应用的 migration，Flyway 用 checksum 校验，改一个字节生产就启动不来。
    /// 这条断言把「别去改历史迁移」这个纪律钉在测试里——有人真去改了会立刻红。
    @Test
    void historicalMigrationsKeepOldNamesUntouched() throws IOException {
        String v9 = read("V9__achievement_baseline.sql");
        assertThat(v9).as("V9 必须原样保留旧类目名——它是已应用的迁移，checksum 不能变")
                .contains("'入职档案'")
                .contains("'机房作业'")
                .doesNotContain("'开卡入场'");
    }
}
