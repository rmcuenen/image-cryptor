package cuenen.raymond.image;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;

/**
 * Composite class to be used to blend two images together based on the alpha
 * component. <br>
 * The {@link #Hidden} composite can be used the fully hide the second image.
 * The {@link #Shown} composite can be used to fully show the second image,
 * making the first image semi-transparent. The composite {@link #getInstance}
 * with any opacity value between 0 and 1 will show the two images blend
 * together based on the given value.
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public final class HiddenMask implements Composite {

    /**
     * Composite used for hiding the image.
     */
    public static final HiddenMask Hidden = new HiddenMask(0f);

    /**
     * Composite used for showing the image.
     */
    public static final HiddenMask Shown = new HiddenMask(1f);

    /**
     * The alpha value, or opacity, of this object.
     */
    private final float alpha;

    /**
     * Retrieve the instance of this type of composite for the given opacity.
     *
     * @param opacity The alpha value, or opacity, of this object
     * @return The composite instance created for the given opacity.
     */
    public static HiddenMask getInstance(float opacity) {
        return new HiddenMask(opacity);
    }

    /**
     * Creates a new composite with the given opacity value.
     *
     * @param alpha The alpha value, or opacity, of this object.
     */
    private HiddenMask(float alpha) {
        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException(
                    "alpha must be comprised between 0.0f and 1.0f");
        }
        this.alpha = alpha;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeContext createContext(ColorModel srcColorModel,
            ColorModel dstColorModel, RenderingHints hints) {
        if (!checkComponentsOrder(srcColorModel)
                || !checkComponentsOrder(dstColorModel)) {
            throw new RasterFormatException("Incompatible color models");
        }

        return new HidingContext(this);
    }

    /**
     * Returns the opacity of this composite.
     *
     * @return the alpha value, or opacity, of this object.
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(alpha) * 31;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HiddenMask)) {
            return false;
        }

        final HiddenMask hm = (HiddenMask) obj;
        return alpha == hm.alpha;
    }

    /**
     * Checks if the given {@code ColorModel} is usable for this composite.
     *
     * @param cm The {@code ColorModel} to be checked.
     * @return {@code true} if the given {@code ColorModel} is usable for this
     * composite, {@code false} otherwise.
     */
    private static boolean checkComponentsOrder(ColorModel cm) {
        if (cm instanceof DirectColorModel
                && cm.getTransferType() == DataBuffer.TYPE_INT) {
            final DirectColorModel directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x00FF0000
                    && directCM.getGreenMask() == 0x0000FF00
                    && directCM.getBlueMask() == 0x000000FF
                    && (directCM.getNumComponents() != 4 || directCM
                    .getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    /**
     * The {@code CompositeContext} class performing the actual pixel
     * composition.
     */
    private static final class HidingContext implements CompositeContext {

        /**
         * Reference to the composite for retrieving the alpha, or blend,
         * factor.
         */
        private final HiddenMask mask;

        /**
         * Create a new {@code CompositeContext} for the given composite.
         *
         * @param mask The {@code HiddenMask} composite to base the pixel
         * composition on.
         */
        private HidingContext(HiddenMask mask) {
            this.mask = mask;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            final int width = Math.min(src.getWidth(), dstIn.getWidth());
            final int height = Math.min(src.getHeight(), dstIn.getHeight());

            final float alpha = mask.getAlpha();

            final int[] result = new int[4];
            final int[] srcPixel = new int[4];
            final int[] dstPixel = new int[4];
            final int[] srcPixels = new int[width];
            final int[] dstPixels = new int[width];

            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                dstIn.getDataElements(0, y, width, 1, dstPixels);
                for (int x = 0; x < width; x++) {
                    // pixels are stored as INT_ARGB
                    // our arrays are [R, G, B, A]
                    int pixel = srcPixels[x];
                    srcPixel[0] = (pixel >> 16) & 0xFF;
                    srcPixel[1] = (pixel >> 8) & 0xFF;
                    srcPixel[2] = (pixel) & 0xFF;
                    srcPixel[3] = (pixel >> 24) & 0xFF;

                    pixel = dstPixels[x];
                    dstPixel[0] = (pixel >> 16) & 0xFF;
                    dstPixel[1] = (pixel >> 8) & 0xFF;
                    dstPixel[2] = (pixel) & 0xFF;
                    dstPixel[3] = (pixel >> 24) & 0xFF;

                    blend(srcPixel, dstPixel, result);

                    // mixes the result with the opacity
                    dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24
                            | ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF) << 16
                            | ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) << 8
                            | (int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF;
                }
                dstOut.setDataElements(0, y, width, 1, dstPixels);
            }
        }

        /**
         * Blend the source pixels with the destination pixels.
         *
         * @param src The pixel color components of the source image.
         * @param dst The pixel color components of the destination image.
         * @param result The pixel color components of the resulting image.
         */
        public void blend(int[] src, int[] dst, int[] result) {
            result[0] = Math.min(src[0], dst[0]);
            result[1] = Math.min(src[1], dst[1]);
            result[2] = Math.min(src[2], dst[2]);
            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
        }
    }
}
