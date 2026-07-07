# Port 与 Service 命名规范

## 命名规范表

| 类型 | 定义层 | 实现层 | 接口命名 | 实现命名 | 本仓实例 |
|------|--------|--------|----------|----------|----------|
| Repository（聚合持久化） | Domain | Infra | `{Entity}Repository` | `{Entity}RepositoryAdapter` | `InteractionRepository` → `InteractionRepositoryAdapter` |
| Driven Port（外部能力/领域动作） | Domain | Infra | `{Function}Port` | `{Function}Adapter`（具体实现可用具体命名） | `DrawPort` → `DrawAdapter`；`ImageStoragePort` → `R2StorageAdapter` |
| ReadPort（CQRS 读投影） | Domain | Infra | `{Entity}ReadPort` | `{Entity}ReadAdapter` | `PromptReadPort` → `PromptReadAdapter`；`RechargeReadPort` → `RechargeReadAdapter` |
| QueryService（查询用例） | — | App | 无接口 | `{View}QueryService` | `PoolQueryService`、`PromptQueryService`、`MyDrawsQueryService` |

patra 另有 LookupPort（装饰器缓存查找）与 Gateway（Domain→App），本仓尚无先例——需要时照 patra `port-service.md` 原表补，不要自造形态。

## 选择指南

```
需要定义的接口是什么类型？
│
├── 聚合/成员表持久化(写为主) ────→ {Entity}Repository（Domain → Infra）
├── 外部能力或领域动作(R2/缩略图/抽奖)→ {Function}Port（Domain → Infra）
├── CQRS 读投影(列表/统计/上游读) ──→ {Entity}ReadPort（Domain → Infra）
└── 纯查询编排(无副作用) ──────────→ {View}QueryService（App 层直接实现,无接口）
```

## 包位置速查

| 类型 | 接口位置 | 实现位置 |
|------|----------|----------|
| Repository | `domain/port/repository/` | `infra/adapter/persistence/` |
| Driven Port | `domain/port/{function}/`（`draw/`、`campaign/`、`storage/`、`thumbnail/`） | `infra/adapter/{能力}/`（写库的落 `persistence/`） |
| ReadPort | `domain/port/read/` | `infra/adapter/read/` |
| QueryService | — | `app/usecase/{子域}/query/` |

## 禁止行为

1. 禁止 Repository 用 `Port` 后缀（~~`InteractionPort`~~）
2. 禁止读投影用 `Repository`/`Query` 后缀命名端口（读端口一律 `{Entity}ReadPort`）
3. 禁止 QueryService 定义接口（CQRS 读端不需要抽象；mock 测试直接 mock 端口）
4. 禁止在 domain 定义接口又在 domain 实现
5. 写操作不进 QueryService——写一律 Command + Handler（见 tech/commandbus.md）
