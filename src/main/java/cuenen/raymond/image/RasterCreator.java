package cuenen.raymond.image;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for scrambling and de-scrambling images. <br>
 * The public scramble method takes:
 * <ul>
 * <li>An enumeration indicating the swap direction;
 * {@link SwapDirection#FORWARD} for scrambling an image and
 * {@linkplain SwapDirection#BACKWARD} for de-scrambling.</li>
 * <li>An integer representing the seed for the random generator.</li>
 * <li>The source {@link Raster} representing the image.</li>
 * <li>The destination {@link WritableRaster} to write the result into.</li>
 * </ul>
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class RasterCreator {

    /**
     * Enumeration type indication to forward scramble or backward
     * (de-)scramble.
     */
    public enum SwapDirection {

        /**
         * Forward swap or scrambling.
         */
        FORWARD,
        /**
         * Backward swap or de-scrambling.
         */
        BACKWARD;
    }

    /**
     * Private constructor so it is not instantiated.
     */
    private RasterCreator() {
    }

    /**
     * Convenience method to scramble an image.
     *
     * @see #create
     * @param seed The start seed for the random generator.
     * @param src The source raster.
     * @param dstOut The destination raster to write to.
     */
    public static void scramble(long seed, Raster src, WritableRaster dstOut) {
        create(SwapDirection.FORWARD, seed, src, dstOut);
    }

    /**
     * Convenience method to de-scramble an image.
     *
     * @see #create
     * @param seed The start seed for the random generator.
     * @param src The source raster.
     * @param dstOut The destination raster to write to.
     */
    public static void descramble(long seed, Raster src, WritableRaster dstOut) {
        create(SwapDirection.BACKWARD, seed, src, dstOut);
    }

    /**
     * Creates a scrambled raster from the given source, using the given seed
     * for the random generator and the given direction. The scrambled raster is
     * written to the given destination.
     *
     * @param type The direction of scrambling, one of
     * {@link SwapDirection#FORWARD} (scrambling) or
     * {@link SwapDirection#BACKWARD} (de-scrambling).
     * @param seed The start seed for the random generator.
     * @param src The source raster.
     * @param dstOut The destination raster to write to.
     */
    public static void create(SwapDirection type, long seed, Raster src, WritableRaster dstOut) {
        final List<RasterElement> imageList = new ArrayList<>();
        final int width = src.getWidth();
        final int height = src.getHeight();
        final int[] srcPixels = new int[width];

        /* Read the image */
        for (int y = 0; y < height; y++) {
            src.getDataElements(0, y, width, 1, srcPixels);
            for (int x = 0; x < width; x++) {
                /* pixels are stored as INT_ARGB our arrays are [R, G, B, A] */
                final int pixel = srcPixels[x];
                final RasterElement elem = new RasterElement();
                elem.pixel[0] = (pixel >> 16) & 0xFF;
                elem.pixel[1] = (pixel >> 8) & 0xFF;
                elem.pixel[2] = (pixel) & 0xFF;
                elem.pixel[3] = (pixel >> 24) & 0xFF;
                imageList.add(elem);
            }
        }

        final List<SwapElement> swapList = new ArrayList<>();
        final RandomNumberGenerator rand = new RandomNumberGenerator(seed);
        int N = imageList.size();
        for (int leftIndex = 0; leftIndex < N; leftIndex++) {
            /* Choose a random index, where leftIndex < rightIndex < N */
            final int rightIndex = leftIndex + (int) rand.random(0, N - leftIndex);
            final SwapElement elem = new SwapElement();
            elem.leftIndex = leftIndex;
            elem.rightIndex = rightIndex;
            swapList.add(elem);
        }

        /* Perform the swaps */
        int index;
        for (int i = 0; i < N; i++) {
            index = type == SwapDirection.FORWARD ? i : N - i - 1;
            final SwapElement elem = swapList.get(index);
            swap(elem.leftIndex, elem.rightIndex, imageList);
        }

        /* Create the image */
        index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final RasterElement elem = imageList.get(index++);
                srcPixels[x] = (255 & 0xFF) << 24
                        | (elem.pixel[0] & 0xFF) << 16
                        | (elem.pixel[1] & 0xFF) << 8 | elem.pixel[2] & 0xFF;
            }
            dstOut.setDataElements(0, y, width, 1, srcPixels);
        }
    }

    /**
     * Method to perform a swap in the {@code imageList}.
     *
     * @param leftIndex The left index to be swapped with the right index.
     * @param rightIndex The right index to be swapped with the left index.
     * @param imageList The list of {@code RasterElement}s representing the
     * image in memory.
     */
    private static void swap(int leftIndex, int rightIndex, List<RasterElement> imageList) {
        final RasterElement leftElement = imageList.get(leftIndex);
        final RasterElement rightElement = imageList.get(rightIndex);
        imageList.set(rightIndex, leftElement);
        imageList.set(leftIndex, rightElement);
    }

    /**
     * Convenience class that holds an integer array indicating the four pixel
     * color components.
     */
    private static class RasterElement {

        public final int[] pixel = new int[4];
    }

    /**
     * Convenience class that holds two integers indicating the left and right
     * index to be swapped.
     */
    private static class SwapElement {

        public int leftIndex, rightIndex;
    }
}
