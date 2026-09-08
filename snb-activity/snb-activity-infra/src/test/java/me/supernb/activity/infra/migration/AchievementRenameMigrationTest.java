package me.supernb.activity.infra.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/// 成就文案重命名迁移（V15 网吧化 / V22 回退）的**静态**完整性校验（不连库、不需要 Docker）。
///
/// 为什么值得单独写：`UPDATE ... WHERE code = 'xxx'` 拼错一个 code 会**影响 0 行且不报错**——
/// 那条成就静默保留旧名字，要等用户在成就墙上看见才发现。类目同理。
/// 这类「静默漏改」是纯 SQL 迁移最容易翻车的地方，编译器和 Flyway 都拦不住。
///
/// V22（苹果式换代批 5，2026-09-07）把 V15 逐条回退到 V9/V10 原文，断言口径也就多一层：
/// 不只是「覆盖了每个 code」，而是**改回去的值逐字等于 V9/V10**——回退迁移抄错一个字
/// （尤其 V9 用的是半角逗号/冒号，V15 换成了全角）静态上同样没人拦得住。
class AchievementRenameMigrationTest {

    private static final Pattern V9_CODE = Pattern.compile("^\\(9\\d{5}, '([a-z0-9_]+)'", Pattern.MULTILINE);
    private static final Pattern V9_CATEGORY =
            Pattern.compile("^\\(9\\d{5}, '[a-z0-9_]+', (?:NULL|'[^']*'), (?:NULL|\\d+), '([^']+)'",
                    Pattern.MULTILINE);
    private static final Pattern V15_CODE = Pattern.compile("WHERE code = '([a-z0-9_]+)'");
    private static final Pattern V15_CATEGORY = Pattern.compile("WHERE category = '([^']+)'");

    /// V9 的 seed 行固定三行：第 1 行 id/code/…/category，第 3 行 name, condition_text, flavor_text, hint。
    private static final Pattern V9_ROW = Pattern.compile(
            "^\\(9\\d{5}, '([a-z0-9_]+)',[^\n]*\n[^\n]*\n '([^']*)', '[^']*', (?:'([^']*)'|NULL), (?:'[^']*'|NULL)\\)[,;]$",
            Pattern.MULTILINE);
    /// V10 的 seed 行：('series_code', 'series_name'),
    private static final Pattern V10_SERIES = Pattern.compile("\\('([a-z_]+)', '([^']*)'\\)");
    /// V15/V22 共用的 name+flavor 改写形状（两行一条）。
    private static final Pattern RENAME_NAME_FLAVOR = Pattern.compile(
            "SET name = '([^']*)',\\s*\n\\s*flavor_text = '([^']*)' WHERE code = '([a-z0-9_]+)';");
    /// ⚠️ `\\s+` 不是 `' '`：V15 为对齐在 SET 与 WHERE 之间塞了多个空格，
    ///    单空格的正则在 V15 上会一条都匹配不到（本条断言初版实测踩过）。
    private static final Pattern SET_CATEGORY =
            Pattern.compile("SET category = '([^']+)'\\s+WHERE category = '([^']+)';");
    private static final Pattern SET_SERIES_NAME =
            Pattern.compile("SET series_name = '([^']*)'\\s+WHERE series_code = '([a-z_]+)';");

    private static final String V9 = "V9__achievement_baseline.sql";
    private static final String V10 = "V10__achievement_series_label.sql";
    private static final String V15 = "V15__achievement_wangba_rename.sql";
    private static final String V22 = "V22__achievement_apple_revert.sql";

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

    /// 🚨 V9/V10/V15 都是已应用的 migration，Flyway 用 checksum 校验，改一个字节生产就启动不来。
    /// 这条断言把「别去改历史迁移」这个纪律钉在测试里——有人真去改了会立刻红。
    /// V22 的回退是**新增迁移**，不是"把 V15 改回去"；谁想省事直接编辑 V15 会被下半段拦住。
    @Test
    void historicalMigrationsKeepOldNamesUntouched() throws IOException {
        String v9 = read("V9__achievement_baseline.sql");
        assertThat(v9).as("V9 必须原样保留旧类目名——它是已应用的迁移，checksum 不能变")
                .contains("'入职档案'")
                .contains("'机房作业'")
                .doesNotContain("'开卡入场'");
        String v15 = read(V15);
        assertThat(v15).as("V15 必须原样保留网吧化后的值——回退走 V22 新增迁移，不是改 V15")
                .contains("SET category = '开卡入场'")
                .contains("SET category = '充网费记录'")
                .contains("SET series_name = '局数系列 · SESSIONS'");
    }

    /// 类目名被代码/数据当**查询键**用的地方，换名一个都不能漏——初版 V15 只改了 category 列，
    /// 漏了 prerequisite 列（meta_category_onboarding 的判定引擎按它现查类目成就，旧名筛出
    /// 空集该成就永不解锁）与 RechargeAchievementJudgeJob 的 category 过滤串，全靠 boot
    /// 全链矩阵(真库)才抓出来。这条把 prerequisite 覆盖钉死，防未来再加"类目引用列"时重蹈。
    @Test
    void v15AlsoRenamesCategoryReferencesStoredInPrerequisiteColumn() throws IOException {
        String v15 = read("V15__achievement_wangba_rename.sql");
        assertThat(v15).as("V9 里唯一的 prerequisite 类目引用(900041)必须被 V15 改成新名")
                .contains("SET prerequisite = '开卡入场'")
                .contains("WHERE prerequisite = '入职档案'");
    }

    // ══════════════════════════════════════════════════════════════════════
    // V22：网吧化回退（苹果式换代批 5）——值必须**逐字**等于 V9/V10 原文
    // ══════════════════════════════════════════════════════════════════════

    /// 表驱动：从 V9 现场解析 42 条 (code → name, flavor_text) 当期望值，与 V22 的改写逐条比。
    /// 不手抄期望值——手抄本身就是这条断言要防的那种错。
    @Test
    void v22RevertsEveryNameAndFlavorToV9Wording() throws IOException {
        Map<String, String[]> v9 = new LinkedHashMap<>();
        Matcher m = V9_ROW.matcher(read(V9));
        while (m.find()) {
            v9.put(m.group(1), new String[] {m.group(2), m.group(3)});
        }
        assertThat(v9).as("V9 应解析出 42 条成就的 name/flavor_text").hasSize(42);

        Map<String, String[]> v22 = new LinkedHashMap<>();
        Matcher r = RENAME_NAME_FLAVOR.matcher(read(V22));
        while (r.find()) {
            assertThat(v22).as("同一个 code 在 V22 被改两次，后一条会覆盖前一条").doesNotContainKey(r.group(3));
            v22.put(r.group(3), new String[] {r.group(1), r.group(2)});
        }
        assertThat(v22.keySet()).as("V22 必须覆盖 V9 的全部 42 个 code，一个不漏").isEqualTo(v9.keySet());

        v9.forEach((code, expected) -> {
            assertThat(v22.get(code)[0]).as("%s 的 name 必须逐字回到 V9 原文", code).isEqualTo(expected[0]);
            assertThat(v22.get(code)[1]).as("%s 的 flavor_text 必须逐字回到 V9 原文（含半角逗号/冒号）", code)
                    .isEqualTo(expected[1]);
        });
    }

    /// 类目：V22 的 8 条必须是 V15 的**精确反向**（V15 的新名当条件、V9 的旧名当目标值）。
    @Test
    void v22RevertsEveryCategoryRenameDoneByV15() throws IOException {
        Map<String, String> v15 = new LinkedHashMap<>();
        Matcher a = SET_CATEGORY.matcher(read(V15));
        while (a.find()) {
            v15.put(a.group(2), a.group(1)); // 旧名 → 新名
        }
        Map<String, String> v22 = new LinkedHashMap<>();
        Matcher b = SET_CATEGORY.matcher(read(V22));
        while (b.find()) {
            v22.put(b.group(2), b.group(1)); // 新名 → 旧名
        }
        assertThat(v15).as("V15 改了 8 个类目").hasSize(8);
        assertThat(v22).as("V22 必须逐条改回去").hasSize(8);
        v15.forEach((v9Name, wangbaName) -> assertThat(v22.get(wangbaName))
                .as("类目 %s 必须被 V22 改回 %s", wangbaName, v9Name)
                .isEqualTo(v9Name));

        Set<String> v9Cats = new LinkedHashSet<>(matches(V9_CATEGORY, read(V9)));
        assertThat(new LinkedHashSet<>(v22.values())).as("V22 的目标类目名必须正好是 V9 实际使用的 8 个")
                .isEqualTo(v9Cats);
    }

    /// 系列标题：V22 的 9 条必须逐字等于 V10 原文；appreciation V15 没改过，V22 也不该出现。
    @Test
    void v22RevertsSeriesLabelsToV10Wording() throws IOException {
        Map<String, String> v10 = new LinkedHashMap<>();
        Matcher m = V10_SERIES.matcher(read(V10));
        while (m.find()) {
            v10.put(m.group(1), m.group(2));
        }
        assertThat(v10).as("V10 应 seed 10 条系列标题").hasSize(10);

        Map<String, String> v15 = new LinkedHashMap<>();
        Matcher a = SET_SERIES_NAME.matcher(read(V15));
        while (a.find()) {
            v15.put(a.group(2), a.group(1));
        }
        Map<String, String> v22 = new LinkedHashMap<>();
        Matcher b = SET_SERIES_NAME.matcher(read(V22));
        while (b.find()) {
            v22.put(b.group(2), b.group(1));
        }
        assertThat(v22.keySet()).as("V22 回退的系列必须与 V15 改过的那 9 个一一对应").isEqualTo(v15.keySet());
        v22.forEach((code, name) -> assertThat(name).as("系列 %s 的标题必须逐字回到 V10 原文", code)
                .isEqualTo(v10.get(code)));
        assertThat(v22).as("appreciation 的标题 V15 没动过，V22 也不该动").doesNotContainKey("appreciation");
    }

    /// 类目名被当**查询键**的两处派生引用（prerequisite 列、condition_text 里的「点亮「X」全部成就」）
    /// 一起回退——只回 category 列会让「档案齐全」永不解锁（V15 初版原样踩过，见上一条注释）。
    @Test
    void v22AlsoRevertsCategoryReferencesInPrerequisiteAndConditionText() throws IOException {
        String v22 = read(V22);
        assertThat(v22).as("prerequisite 列（V9:900041 meta_category_onboarding）必须回 V9 原名")
                .contains("SET prerequisite = '入职档案'")
                .contains("WHERE prerequisite = '开卡入场'");
        assertThat(v22).as("condition_text 里的类目引用必须回 V9 原文")
                .contains("'点亮「开卡入场」全部成就', '点亮「入职档案」全部成就'");
    }

    /// V22 是回退迁移：不许夹带任何 DDL / 非 UPDATE 的写操作（本批只换文案，不动结构与判定字段）。
    @Test
    void v22IsUpdateOnlyAndTouchesNoSchema() throws IOException {
        String sql = read(V22).lines()
                .filter(l -> !l.stripLeading().startsWith("--"))
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining("\n"));
        assertThat(sql).as("回退迁移只能是 UPDATE，出现建表/加列/删数据一律拦下")
                .doesNotContainIgnoringCase("CREATE ")
                .doesNotContainIgnoringCase("ALTER ")
                .doesNotContainIgnoringCase("DROP ")
                .doesNotContainIgnoringCase("DELETE ")
                .doesNotContainIgnoringCase("INSERT ");
        long updates = read(V22).lines().filter(l -> l.startsWith("UPDATE ")).count();
        assertThat(updates).as("V22 的 UPDATE 条数必须与 V15 一一对应（8 类目 + 9 系列 + 42 文案 + 1 条件 + 1 前置）")
                .isEqualTo(61);
    }
}
