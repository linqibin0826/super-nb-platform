package me.supernb.gallery.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageIoThumbnailAdapterTest {

    private final ImageIoThumbnailAdapter adapter = new ImageIoThumbnailAdapter();

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void shrinksLongEdgeToMaxKeepingRatio() throws Exception {
        byte[] thumb = adapter.toPng(png(512, 256), 256);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(thumb));
        assertThat(out.getWidth()).isEqualTo(256);
        assertThat(out.getHeight()).isEqualTo(128);
    }

    @Test
    void doesNotUpscaleSmallImages() throws Exception {
        byte[] thumb = adapter.toPng(png(100, 80), 256);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(thumb));
        assertThat(out.getWidth()).isEqualTo(100);
        assertThat(out.getHeight()).isEqualTo(80);
    }

    @Test
    void badImageThrows() {
        assertThatThrownBy(() -> adapter.toPng(new byte[] {1, 2, 3}, 256))
                .isInstanceOf(RuntimeException.class);
    }
}
