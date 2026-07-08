# super-nb-studio — 创作工坊 `studio.super-nb.me`

`https://studio.super-nb.me/` 的源码仓库。独立 React SPA（Vite + React 19 + TS + Tailwind，明暗双主题），一期承载 gpt-image-2 生图（登录态与主站经父域 cookie `__Secure-snb_auth` 无感 SSO），二期同站挂 tldraw 画布。设计稿与 runbook 见 `../ai-relay`（spec `2026-07-04-creative-studio-design.md`、runbook `deployment/18`、`deployment/19`；上线后优化 spec/plan `docs/superpowers/{specs,plans}/2026-07-05-studio-post-launch-optimization*`）。

## 本地开发

```bash
pnpm dev   # http://localhost:3100/studio/
```

依赖两个本地条件：

- **本地 sub2api 栈**跑在 `127.0.0.1:8080`（`/api`、`/v1` 由 vite 代理过去）；
- **同级 `../super-nb-gallery` 仓库**存在（dev 中间件直读它提供 `/playground-gallery/*`；生产灵感库数据走 gallery-svc `/gallery/api/*`）。

⚠️ dev 端口与主站不同源，cookie 不共享——登录态要手工种（见 ai-relay runbook 18 的验证脚本）；生产子域名下父域 cookie 自动可见。

## 发布

```bash
./deploy.sh   # STUDIO_BASE=/ build + rsync → us-new:/root/sub2api/deploy/caddy_config/web/studio/（零发版零重启）
```

目标路径写死不接受参数。线上是独立子域名站点块 `studio.super-nb.me { ... }`（非 `api.super-nb.me` 子路径）——Caddy 拓扑见 ai-relay `deployment/18`。

⚠️ 发版前必须 `pnpm build && pnpm preview` 手验 dist：`@super-nb/ui` 走 `link:` 联包，双 React 实例会导致生产 bundle 白屏（`useId` null），`vite.config.ts` 的 `resolve.dedupe` 是唯一防线，dev 正常不代表生产正常。

## 门禁

`pnpm typecheck` 绿 + `pnpm test:run` 全绿。`src/lib/` 模块拷自 sub2api fork（`feat/image-playground@4f873b56`），与 fork 同步演进须手动。设计系统组件来自 `../super-nb-ui`（`link:`），改 ui 后须在该仓 `pnpm build` 本仓才可见。
