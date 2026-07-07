package me.supernb.gallery.infra.adapter.thumbnail;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import me.supernb.gallery.domain.port.thumbnail.ThumbnailPort;
import org.springframework.stereotype.Component;

/// [ThumbnailPort] 实现:JDK 内置 ImageIO 生成缩略图,统一输出 PNG。
///
/// 故意不出 webp——ImageIO 原生不带 webp 编码,引入等于多背一个依赖;前端只消费缩略图 URL,
/// 不关心具体编码格式,输出格式与原图格式是否一致不影响功能。
@Component
public class ImageIoThumbnailAdapter implements ThumbnailPort {

    /// 原图字节 → 长边缩到 maxEdge 的 PNG 字节,保持宽高比、只缩不放。解码失败(非受支持的图片格式)
    /// 抛 IllegalArgumentException,编解码 IO 异常包成 UncheckedIOException——均为 RuntimeException,
    /// 呼应 [ThumbnailPort] 契约:坏图抛运行期异常,由调用方尽力而为(捕获后放弃缩略图,不阻断主流程)。
    @Override
    public byte[] toPng(byte[] src, int maxEdge) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(src));
            if (img == null) {
                throw new IllegalArgumentException("无法解码图片");
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double scale = Math.min(1.0, (double) maxEdge / Math.max(w, h)); // scale 封顶 1.0,只缩不放
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
