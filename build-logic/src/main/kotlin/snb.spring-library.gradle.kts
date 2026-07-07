// 需要 Spring BOM 但非 Boot 应用的模块(app/infra/adapter/common/sub2api)。
// BOM 已由 snb.java-base 的 applySnbDependencyManagement 引入。
plugins {
    id("snb.java-library")
}
