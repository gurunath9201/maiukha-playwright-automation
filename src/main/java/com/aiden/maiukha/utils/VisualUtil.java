package com.aiden.maiukha.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Very small pixel-comparison helper for visual regression testing. */
public final class VisualUtil {

    private VisualUtil() { }

    /** @return percentage of pixels that differ (100 if sizes differ). */
    public static double diffPercent(Path a, Path b) throws IOException {
        BufferedImage img1 = ImageIO.read(a.toFile());
        BufferedImage img2 = ImageIO.read(b.toFile());
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return 100.0;
        }
        long diff = 0;
        long total = (long) img1.getWidth() * img1.getHeight();
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    diff++;
                }
            }
        }
        return diff * 100.0 / total;
    }
}