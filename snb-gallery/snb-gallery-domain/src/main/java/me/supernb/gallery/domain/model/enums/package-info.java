/// gallery 领域枚举存放处,当前只有一个类型:[SortMode](提示词列表排序模式)。
///
/// SortMode.from(String) 把请求排序参数解析成枚举,经 app 层 PromptQueryService 传给
/// domain/port/read 的 PromptReadPort,最终由 infra 的 PromptReadAdapter.orderHql() 映射成
/// ORDER BY 片段——枚举本身不依赖任何实现细节。
package me.supernb.gallery.domain.model.enums;
