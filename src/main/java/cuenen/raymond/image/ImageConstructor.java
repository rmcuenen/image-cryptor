package cuenen.raymond.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The {@code ImageConstructor} class is used to create a de-scrambled image
 * from a given scrambled image. <br>
 * De-scrambling is done by the same shuffle-algorithm as for creating a
 * scrambled image with the {@code ImageCreator}. The seed of the random number
 * generator used for scrambling is used for de-scrambling. the image again.
 *
 * @see RasterCreator
 * @see ImageCreator
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class ImageConstructor {

    /**
     * The de-scrambled image in memory.
     */
    private final BufferedImage image;

    /**
     * Perform a de-scramble action on the image indicated by the given {@code
     * imageFile} and use the given {@code aFrame} as parent frame for the
     * dialog.
     *
     * @param imageFile The URL indicating the image to be de-scrambled.
     */
    public ImageConstructor(URL imageFile) {
        BufferedImage dst;
        try {
            final BufferedImage src = GraphicsUtilities
                    .loadCompatibleImage(imageFile);
            final long seed = readSeed(imageFile.openStream());
            dst = new BufferedImage(src.getWidth(),
                    src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            /* De-scramble the source image */
            RasterCreator.descramble(seed, src.getData(), dst.getRaster());
        } catch (IOException e) {
            dst = null;
            System.err.println("Error: " + e.getMessage());
        }
        image = dst;
    }

    /**
     * Method to retrieve the de-scrambled image.
     *
     * @return The de-scrambled image in memory.
     */
    public BufferedImage getImage() {
        return GraphicsUtilities.toCompatibleImage(image);
    }

    /**
     * Read the PNG meta-data and extract the seed used for scrambling the
     * source image.
     *
     * @param input The {@link java.io.InputStream} to the image.
     * @return The seed used for scrambling the source image.
     * @throws java.io.IOException If the image cannot be read, or the seed is
     * not present.
     */
    private long readSeed(InputStream input) throws IOException {
        final Map<String, String> metadata = new HashMap<>();
        final ImageReader imageReader = ImageIO.getImageReadersByFormatName("png").next();
        final ImageInputStream stream = ImageIO.createImageInputStream(input);
        imageReader.setInput(stream, true);
        final IIOMetadata meta = imageReader.getImageMetadata(0);
        final List<Node> text = new ArrayList<>();
        findNodes(meta.getAsTree("javax_imageio_1.0"), text);
        for (Node node : text) {
            NamedNodeMap textEntry = node.getAttributes();
            String key = textEntry.getNamedItem("keyword").getNodeValue();
            String value = textEntry.getNamedItem("value").getNodeValue();
            metadata.put(key, value);
        }
        input.close();
        try {
            return Long.parseLong(metadata.get("seed"));
        } catch (NullPointerException | NumberFormatException ex) {
            throw new IOException(ex);
        }
    }

    private void findNodes(Node root, List<Node> list) {
        if (root.getNodeName().equals("TextEntry")) {
            list.add(root);
        }
        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            findNodes(children.item(i), list);
        }
    }
}
