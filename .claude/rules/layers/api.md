---
paths: snb-*/snb-*-api/**/*.java
---

# API 模块开发规范

## 核心职责

- 上下文**对外契约**:DTO、接口定义、常量——patra 微服务里是服务间 RPC 契约,本平台单体形态下即「跨上下文调用契约」
- 当前为**空壳预留**:上下文之间零互调。第一笔跨上下文调用出现时,被调方把对外契约放进自己的 api 模块,调用方 infra 依赖该 api 做薄适配(自己 app 层定义端口,infra 映射)

## 禁止行为

1. 禁止包含 Controller 实现
2. 禁止包含业务逻辑
3. 禁止依赖 domain / app / infra / adapter(ArchUnit 门禁 `apiIsContractOnly`)
4. 🚨 上下文之间照旧禁止直接依赖彼此的 domain/app——跨上下文只经 api 契约
