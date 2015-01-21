package cuenen.raymond.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Executing class to create a scrambled image from a given image. <br>
 * Scrambling is done by a simple shuffle-algorithm based on random numbers. The
 * {@code seed} of the random number generator is important for de-scrambling
 * the image again.
 *
 * @see RasterCreator
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class ImageCreator {

    /**
     * Flag indicating to use a random seed.
     */
    public static final int RANDOMSEED = -1;

    private static final String METADATA_FORMAT = "javax_imageio_1.0";

    /**
     * Perform a scramble action on the image indicated by the given {@code
     * imageFile} and use the given {@code seed} for the scramble algorithm.
     *
     * @param imageFile The filename indicating the image to be scrambled.
     * @param seed The seed for the random generator or {@link #RANDOMSEED}.
     */
    public ImageCreator(String imageFile, long seed) {
        final RandomNumberGenerator rand = new RandomNumberGenerator(seed == RANDOMSEED
                ? new Random(System.currentTimeMillis()).nextLong() : seed);
        final long num = rand.seed();
        System.out.println("Using random seed: " + num);
        try {
            System.out.println("Reading source image");
            final BufferedImage src = GraphicsUtilities.loadCompatibleImage(new File(imageFile).toURI().toURL());
            final BufferedImage dst = new BufferedImage(src.getWidth(),
                    src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            System.out.println("Scramble image");
            RasterCreator.scramble(seed, src.getData(), dst.getRaster());
            saveImage(imageFile, num, dst);
            System.out.println("Done");
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Save the scrambled image in memory to a file. <br>
     * The {@code seed} used for scrambling will be included in the filename.
     *
     * @param imgName The name of the image file.
     * @param seed The seed used to start the random generator.
     * @param image The scrambled image in memory.
     */
    private void saveImage(String imgName, long seed, BufferedImage image) {
        if (image != null) {
            int index = imgName.lastIndexOf('.');
            index = index == -1 ? imgName.length() : index;
            final String type = "png";
            final String name = imgName.substring(0, index) + "." + type;
            final File imageFile = new File(name);
            System.out.println("Save image as: " + type);
            try {
                final ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
                final ImageWriteParam writeParam = writer.getDefaultWriteParam();
                final IIOMetadata meta = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
                final DOMImplementation dom = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0");
                final Document doc = dom.createDocument(null, METADATA_FORMAT, null);
                final Node tree = doc.getDocumentElement();
                final Node text = doc.createElement("Text");
                tree.appendChild(text);
                final Element textEntry = doc.createElement("TextEntry");
                textEntry.setAttribute("keyword", "seed");
                textEntry.setAttribute("value", String.valueOf(seed));
                text.appendChild(textEntry);
                meta.mergeTree(METADATA_FORMAT, tree);
                final ImageOutputStream stream = ImageIO.createImageOutputStream(imageFile);
                writer.setOutput(stream);
                try {
                    writer.write(meta, new IIOImage(image, null, meta), writeParam);
                } finally {
                    writer.dispose();
                    stream.flush();
                    stream.close();
                }
            } catch (IOException | ClassNotFoundException |
                    InstantiationException | IllegalAccessException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * Creates a scrambled image from the given image file.
     *
     * @param args The string array containing the filename and optionally the
     * random seed.
     */
    public static void main(String[] args) {
        long seed = RANDOMSEED;
        if (args.length > 1) {
            try {
                seed = Long.parseLong(args[1]);
                System.out.println("Seed from command line: " + seed);
            } catch (NumberFormatException ex) {
            }
        }
        if (args.length > 0) {
            ImageCreator imageCreator = new ImageCreator(args[0], seed);
        } else {
            System.out.println("usage: java "
                    + ImageCreator.class.getCanonicalName()
                    + " <filename> [seed]");
        }
    }
}
