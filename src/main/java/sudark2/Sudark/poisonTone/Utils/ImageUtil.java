package sudark2.Sudark.poisonTone.Utils;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class ImageUtil {

    private static final int width = 8;
    private static final int height = 8;

    public static long aHash(BufferedImage src) {

        BufferedImage resized = resize(src, width, height);
        BufferedImage gray = toGray(resized);

        int[] pixels = new int[width * height];
        int sum = 0;
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = gray.getRGB(x, y);
                int grayValue = rgb & 0xFF;
                pixels[index++] = grayValue;
                sum += grayValue;
            }
        }

        int avg = sum / pixels.length;

        long hash = 0L;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > avg) {
                hash |= (1L << i);
            }
        }

        return hash;
    }

    public static int colorHash(BufferedImage src) {

        BufferedImage resized = resize(src, width, height);

        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        int total = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = resized.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                sumR += r;
                sumG += g;
                sumB += b;
            }
        }

        int avgR = (int) (sumR / total);
        int avgG = (int) (sumG / total);
        int avgB = (int) (sumB / total);

        int qR = avgR >> 4;
        int qG = avgG >> 4;
        int qB = avgB >> 4;

        return (qR << 8) | (qG << 4) | qB;
    }

    private static BufferedImage resize(BufferedImage src, int width, int height) {
        Image scaled = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return result;
    }

    private static BufferedImage toGray(BufferedImage src) {
        BufferedImage gray = new BufferedImage(
                src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(src, gray);
        return gray;
    }
}
