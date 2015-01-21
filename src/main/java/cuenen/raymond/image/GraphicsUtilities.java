package cuenen.raymond.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Utility class with a few image processing utilities.
 * <ul>
 * <li> {@link #getFasterScaledInstance} returns a scaled image from the given
 * image.</li>
 * <li> {@link #loadCompatibleImage} loads the image from the given URL.</li>
 * <li> {@link #toCompatibleImage} copies an image into a hardware optimized
 * image.
 * </ul>
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class GraphicsUtilities {

    /**
     * The default {@code GraphicsConfiguration} for the systems hardware.
     */
    private static final GraphicsConfiguration CONFIGURATION = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getDefaultConfiguration();

    /**
     * Private constructor so it is not instantiated.
     */
    private GraphicsUtilities() {

    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * BufferedImage.
     *
     * @param img The original image to be scaled.
     * @param targetWidth The desired width of the scaled instance, in pixels.
     * @param targetHeight The desired height of the scaled instance, in pixels.
     * @param hint One of the rendering hints that corresponds to
     * RenderingHints.KEY_INTERPOLATION (e.g.
     * RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
     * RenderingHints.VALUE_INTERPOLATION_BILINEAR,
     * RenderingHints.VALUE_INTERPOLATION_BICUBIC).
     * @param progressiveBilinear If <code>true</code>, this method will use a
     * multi-step scaling technique that provides higher quality than the usual
     * one-step technique (only useful in down-scaling cases, where targetWidth
     * or targetHeight is smaller than the original dimensions).
     * @return A scaled version of the original BufferedImage.
     */
    public static BufferedImage getFasterScaledInstance(
            BufferedImage img,
            int targetWidth,
            int targetHeight,
            Object hint,
            boolean progressiveBilinear) {
        final int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        int w, h;
        int prevW = ret.getWidth();
        int prevH = ret.getHeight();
        final boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

        if (progressiveBilinear) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // Use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;

            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        if (g2 != null) {
            g2.dispose();
        }

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;
    }

    /**
     * Returns a new compatible image from a URL. The image is loaded from the
     * specified location and then turned, if necessary into a compatible image.
     *
     * @see #toCompatibleImage(java.awt.image.BufferedImage)
     * @param resource The URL of the picture to load as a compatible image.
     * @return A new translucent compatible <code>BufferedImage</code> of the
     * specified width and height.
     * @throws java.io.IOException If the image cannot be read or loaded.
     */
    public static BufferedImage loadCompatibleImage(URL resource)
            throws IOException {
        BufferedImage image = ImageIO.read(resource);
        return toCompatibleImage(image);
    }

    /**
     * Return a new compatible image that contains a copy of the specified
     * image. This method ensures an image is compatible with the hardware, and
     * therefore optimized for fast operations.
     *
     * @see #loadCompatibleImage(java.net.URL)
     * @param image The image to copy into a new compatible image.
     * @return A new compatible copy, with the same width and height and
     * transparency and content, of <code>image</code>.
     */
    public static BufferedImage toCompatibleImage(BufferedImage image) {
        if (image.getColorModel().equals(CONFIGURATION.getColorModel())) {
            return image;
        }

        BufferedImage compatibleImage = CONFIGURATION.createCompatibleImage(
                image.getWidth(), image.getHeight(), image.getTransparency());
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return compatibleImage;
    }
}
