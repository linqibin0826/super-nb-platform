package me.supernb.activity.app.usecase.achievement.config;

import java.util.Set;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/// 深化稿不可逆决策清单第 7 条:category/status/predicate_kind/comparator 禁用 DB 原生
/// ENUM,一律 VARCHAR——运行期唯一能真正"拦得住"的一条不可逆决策,靠这个启动期扫描落实。
/// 白名单以内容表(activity.achievement_definition)当前 42+ 行为准,校验失败视为部署阻断级
/// 问题(内容写错比代码写错更容易被忽视——错别字类目名不会导致编译失败,只会让整条成就永远
/// 分组错误甚至判定失效),因此直接抛异常让应用启动失败,不做"记 WARN 带病运行"这种妥协。
@Component
public class AchievementContentWhitelist implements InitializingBean {

    // 深化稿 §3 开篇:"类目共 8 个启用 + 2 个占位"。占位类目当前 0 行使用,但字面允许——
    // 一旦运营真的往"情报站"/"限时特刊"里加行,不需要先改这份白名单。
    //
    // 2026-09-07 苹果式换代(批 5):V22 把 V15 的网吧化类目名逐条回退到 V9 原文,
    // 「现行」重新是 V9 那一套。两套值继续**同时保留**,不是偷懒。理由:
    //   本校验失败会直接 throw 让应用启动不来(见类注释),而 Flyway migration 是
    //   单向的——发版与回滚两个方向上都可能出现"DB 与镜像各认一套名字"的窗口,
    //   白名单认不出来就会**启动失败**,把一次普通回滚变成生产事故。
    //   保留另一套的代价接近零(它们是本站已知的正当历史值,不是错别字)。
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            // 现行(V9 原文,V22 起回归)
            "入职档案", "机房作业", "补给记录", "联动矩阵", "造像车间", "考勤本纪", "机密档案", "元编年史",
            // 占位(0 行使用,字面允许)
            "情报站", "限时特刊",
            // 🪦 V15~V21 的网吧化类目名,仅为镜像可回滚而保留
            "开卡入场", "上机记录", "充网费记录", "全网联机", "画图机位", "上网时长", "隐藏关卡", "元老档案");
    private static final Set<String> ALLOWED_STATUSES = Set.of("draft", "active", "retired");
    private static final Set<String> ALLOWED_PREDICATE_KINDS =
            Set.of("metric_threshold", "meta_combo", "custom_code");
    private static final Set<String> ALLOWED_COMPARATORS = Set.of("gte", "lte");

    private final AchievementCatalogPort catalogPort;

    public AchievementContentWhitelist(AchievementCatalogPort catalogPort) {
        this.catalogPort = catalogPort;
    }

    /// Boot 启动期自动触发(app 模块 classpath 无 jakarta.annotation,用 Spring 原生
    /// InitializingBean 等价实现,不为一个注解引新依赖)。
    @Override
    public void afterPropertiesSet() {
        validateOnStartup();
    }

    /// 校验主体。返回校验通过的行数(供启动日志打印,非契约,纯诊断用途)。
    public int validateOnStartup() {
        int count = 0;
        for (AchievementDefinition def : catalogPort.allDefinitions()) {
            if (!ALLOWED_CATEGORIES.contains(def.category())) {
                throw new IllegalStateException(
                        "成就目录白名单校验失败:code=" + def.code() + " 的 category 不在白名单内:" + def.category());
            }
            if (!ALLOWED_STATUSES.contains(def.status())) {
                throw new IllegalStateException(
                        "成就目录白名单校验失败:code=" + def.code() + " 的 status 不在白名单内:" + def.status());
            }
            if (!ALLOWED_PREDICATE_KINDS.contains(def.predicateKind())) {
                throw new IllegalStateException("成就目录白名单校验失败:code=" + def.code()
                        + " 的 predicate_kind 不在白名单内:" + def.predicateKind());
            }
            if ("metric_threshold".equals(def.predicateKind()) && !ALLOWED_COMPARATORS.contains(def.comparator())) {
                throw new IllegalStateException(
                        "成就目录白名单校验失败:code=" + def.code() + " 的 comparator 不在白名单内:" + def.comparator());
            }
            count++;
        }
        return count;
    }
}
