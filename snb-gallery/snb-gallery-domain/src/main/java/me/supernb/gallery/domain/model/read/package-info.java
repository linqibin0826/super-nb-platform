/// 读侧视图 record,一文件一类:不属于聚合、无业务逻辑,只承载查询结果的对外形状(patra Read
/// Model 同款)。
///
/// - [Category] / [CategoryNode] / [CategoryTree]:类目(详情内嵌 / 树节点 / 三轴树)
/// - [PromptSummary] / [PromptDetail]:提示词列表瘦身条目 / 全字段详情
/// - [GenerationSummary] / [GenerationDetail] / [Image]:生成历史列表瘦身条目 / 全字段详情 /
///   一张图(url 已现签)
/// - [MyInteractions]:批量点赞/收藏态回填
/// - [Page]:统一分页信封
///
/// 这批 record 会原样透出到 REST 响应,不再另起一套 app 层 DTO 做二次映射——domain 读侧
/// 与对外 API 契约在这里合流,是本仓约定的形态。
package me.supernb.gallery.domain.model.read;
