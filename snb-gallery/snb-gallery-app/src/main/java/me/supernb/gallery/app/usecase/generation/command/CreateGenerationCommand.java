package me.supernb.gallery.app.usecase.generation.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;
import me.supernb.gallery.app.usecase.generation.dto.Created;

/// 创建一条生成记录(base64 已由 adapter 解码为字节)。幂等:本人已有该 id 直接返回原 createdAt。
public record CreateGenerationCommand(
        String id, long userId, String prompt, String size, int n, String quality, String status,
        Double cost, int elapsedMs, String groupName, Long keyId, String error,
        List<ImageBytes> outputImages, List<RefBytes> refImages)
        implements Command<Created> {
}
