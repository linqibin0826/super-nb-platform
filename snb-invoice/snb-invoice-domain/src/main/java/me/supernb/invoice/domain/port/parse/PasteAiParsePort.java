package me.supernb.invoice.domain.port.parse;

import java.util.Optional;

/// 开票资料 AI 识别端口:把用户整段粘贴的自由文本解析成抬头字段。实现方负责防幻觉守卫
/// (数字类字段必须逐字出自原文,对不上一律置 null——LLM 编出来的税号比不填更危险)。
/// 每次调用烧真实上游 token(量级 ~400 tokens/次),调用方必须先过资格闸与配额。
public interface PasteAiParsePort {

    /// 识别结果(全部可空:文本没提到就是 null,绝不编造)。
    record ParsedInfo(String title, String taxNo, String regAddress, String regPhone,
                      String bankName, String bankAccount) {

        /// 是否六字段全空(= 什么都没识别出来)。
        public boolean isEmpty() {
            return title == null && taxNo == null && regAddress == null
                    && regPhone == null && bankName == null && bankAccount == null;
        }
    }

    /// 解析一段文本;empty = 模型明确表示什么都没提取到。通道不可用抛 aiParseUnavailable。
    Optional<ParsedInfo> parse(String blob);
}
