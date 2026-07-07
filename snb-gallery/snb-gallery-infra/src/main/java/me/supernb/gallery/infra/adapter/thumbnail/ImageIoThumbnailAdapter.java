package me.supernb.gallery.infra.adapter.thumbnail;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import me.supernb.gallery.domain.port.ThumbnailPort;
import org.springframework.stereotype.Component;

/// ThumbnailPort 实现:JDK ImageIO 缩放为 PNG(避开 webp 编码依赖;前端只认 URL,格式无所谓)。
@Component
public class ImageIoThumbnailAdapter implements ThumbnailPort {

    @Override
    public byte[] toPng(byte[] src, int maxEdge) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(src));
            if (img == null) {
                throw new IllegalArgumentException("无法解码图片");
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double scale = Math.min(1.0, (double) maxEdge / Math.max(w, h)); // 只缩不放
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
