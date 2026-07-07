package me.supernb.gallery.app.usecase.generation.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;
import me.supernb.gallery.app.usecase.generation.dto.Created;

/// 创建一条生成记录的写命令(图片以字节数组传入,base64 已由 adapter 解码)。
///
/// 不带 id——雪花 id 由 `CreateGenerationHandler` 经 `GenerationRepository.nextId()` 预分配后
/// 再落库(R2 键 `gen/{userId}/{id}/…` 须先于持久化确定,验收意见⑦)。没有创建幂等预检:
/// 重复提交各自落一行,防重复提交靠前端队列的单飞语义兜底,不在服务端做。
///
/// @param userId       生成发起用户 id(sub2api user id)
/// @param prompt       生成提示词
/// @param size         尺寸档(如 `1024x1024`)
/// @param n            出图张数
/// @param quality      画质档
/// @param status       任务终态:`done` | `error`
/// @param cost         本次消耗额度(USD 名义计价)
/// @param elapsedMs    生成耗时毫秒
/// @param groupName    计费分组名
/// @param keyId        使用的 API Key id
/// @param error        失败原因(成功为 NULL)
/// @param outputImages 输出图字节列表(base64 已由 adapter 解码)
/// @param refImages    参考图字节列表(base64 已由 adapter 解码)
public record CreateGenerationCommand(
        long userId, String prompt, String size, int n, String quality, String status,
        Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<ImageBytes> outputImages, List<RefBytes> refImages)
        implements Command<Created> {
}
