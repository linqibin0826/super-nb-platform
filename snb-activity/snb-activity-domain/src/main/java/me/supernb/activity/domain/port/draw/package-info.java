/// `DrawPort`:抽奖原子事务端口,domain/port/{function} 分类下的外部能力/领域动作端口
/// (port-service.md 里举的实例)。由 infra 的 DrawAdapter 实现——per-user advisory lock
/// 串行化 + 单个事务内完成,保证并发下不超额发放;domain/app 只依赖接口语义,不感知 PG
/// 并发原语细节。
package me.supernb.activity.domain.port.draw;
