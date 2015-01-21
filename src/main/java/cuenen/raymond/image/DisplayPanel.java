package cuenen.raymond.image;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 * This panel class is used to display scrambled images that are created with
 * the {@code ImageCreator}. <br>
 * The image is first de-scrambled and will be hidden behind the 'turtle' image.
 * Pressing CTRL + F11 will make the image fully shown, while CTRL + Arrow keys
 * and CTRL + Page Dn/Up control the blend factor. Pressing any other key or
 * combination will immediately hide the image. <br>
 * Images are added to the panel by dragging them onto it for a drop.
 *
 * @version 1.00 Jan 21 2015
 * @author Raymond Cuenen
 */
public class DisplayPanel extends JPanel {

    /**
     * Image to be displayed.
     */
    private BufferedImage image;

    /**
     * Composite used to blend the images.
     */
    private HiddenMask composite = HiddenMask.Hidden;

    /**
     * The source, top, image.
     */
    private final BufferedImage srcImage;

    /**
     * The second image (de-scrambled).
     */
    private BufferedImage dstImage;

    /**
     * Position and size information used for moving top image.
     */
    private int srcX, srcY, dstX, dstY, dstWidth, dstHeight;

    /**
     * Flag indicating that the image needs to be updated.
     */
    private boolean repaint = false;

    /**
     * Creates a new {@code DisplayPanel} for displaying a hidden de-scrambled
     * image.
     *
     * @param imageFile The initial scrambled image to display.
     */
    public DisplayPanel(URL imageFile) {
        BufferedImage topImage;
        try {
            /* Load the top image */
            topImage = GraphicsUtilities.loadCompatibleImage(getClass()
                    .getResource("resources/tortoise.jpg"));
        } catch (IOException e) {
            topImage = new BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_ARGB);
        }
        srcImage = topImage;

        /* Set the initial second image */
        dstImage = new BufferedImage(srcImage.getWidth(), srcImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = dstImage.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, srcImage.getWidth(), srcImage.getHeight());
        g.dispose();
        dstImage = GraphicsUtilities.toCompatibleImage(dstImage);

        /* Enable DnD support for dropping image files */
        setTransferHandler(new ImageTransferHandler());

        /* Set a timer for moving the top image */
        final Timer timer = new Timer("Saver", true);
        timer.scheduleAtFixedRate(new Mover(), 50L, 100L);

        /* Add KeyListener */
        getToolkit().addAWTEventListener((event) -> {
            /* React on the right key events */
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                KeyEvent e = (KeyEvent) event;
                int adjust = 0;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_RIGHT:
                        adjust = 5;
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_LEFT:
                        adjust = -5;
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        adjust = 25;
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        adjust = -25;
                        break;
                    case KeyEvent.VK_F11:
                        adjust = 100;
                        break;
                }
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    final float f = composite.getAlpha() + adjust / 100f;
                    setBlendFactor(Math.max(Math.min(1.0f, f), 0.0f));
                } else if (e.getKeyCode() != KeyEvent.VK_CONTROL) {
                    setBlendFactor(0f);
                }
            }
        }, KeyEvent.KEY_EVENT_MASK);
        enableEvents(ComponentEvent.COMPONENT_EVENT_MASK
                | KeyEvent.KEY_EVENT_MASK);
        setImage(imageFile);
    }

    /**
     * Convenience method to set the blend factor, or opacity, of the image.
     *
     * @param factor The opacity, or blend factor, of the image.
     */
    private void setBlendFactor(float factor) {
        composite = HiddenMask.getInstance(factor);
        repaint = true;
        repaint();
    }

    private boolean setImage(URL imageFile) {
        if (imageFile == null) {
            return false;
        }
        /* Read the file */
        final Window window = SwingUtilities.getWindowAncestor(this);
        final ImageConstructor reader = new ImageConstructor(imageFile);
        dstImage = reader.getImage();
        repaint = true;
        if (window != null) {
            window.pack();
        } else {
            repaint();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void processComponentEvent(ComponentEvent e) {
        super.processComponentEvent(e);
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED) {
            /* Reset the image */
            image = null;
            repaint = true;
            repaint();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        /* Preferred size is the size of the second image */
        return new Dimension(dstImage.getWidth(), dstImage.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (image == null) {
            /* Create the result image */
            image = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            repaint = true;
        }

        if (repaint) {
            /* Update the image */
            dstWidth = dstImage.getWidth();
            dstHeight = dstImage.getHeight();
            final double ratio = (double) getWidth() / (double) getHeight();
            final double imgRatio = (double) dstWidth / (double) dstHeight;
            if (ratio > imgRatio) {
                dstHeight = Math.max(dstHeight, getHeight());
                dstWidth = (int) (imgRatio * dstHeight);
            } else {
                dstWidth = Math.max(dstWidth, getWidth());
                dstHeight = (int) (dstWidth / imgRatio);
            }
            dstX = (getWidth() - dstWidth) / 2;
            dstY = (getHeight() - dstHeight) / 2;
            final Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.white);
            g2.setComposite(AlphaComposite.Src);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.drawImage(srcImage, srcX, srcY, null);
            g2.setComposite(composite);
            g2.drawImage(dstImage, dstX, dstY, dstWidth, dstHeight, null);
            g2.dispose();

            repaint = false;
        }

        g.drawImage(image, 0, 0, null);
    }

    /**
     * TimerTask for animating the movement of the first image.
     */
    private class Mover extends TimerTask {

        private double dirX = 1;
        private double dirY = -1;

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            /*
             * Move the source image, but bounce on the edges of the second
             * image
             */
            final int targetX = (int) (srcX + dirX);
            final int targetY = (int) (srcY + dirY);
            if (targetX + srcImage.getWidth() > dstX + dstWidth
                    || targetX < dstX) {
                dirX = Math.signum(-dirX) * (dirX / dirX);
            } else {
                srcX = targetX;
                dirX += Math.signum(dirX) / 2;
            }
            if (targetY + srcImage.getHeight() > dstY + dstHeight
                    || targetY < dstY) {
                dirY = Math.signum(-dirY) * (dirY / dirY);
            } else {
                srcY = targetY;
                dirY += Math.signum(dirY) / 2;
            }
            srcX = Math.max(dstX, Math.min(srcX, dstX + dstWidth
                    - srcImage.getWidth()));
            srcY = Math.max(dstY, Math.min(srcY, dstY + dstHeight
                    - srcImage.getHeight()));
            repaint = true;
            repaint();
        }
    }

    /**
     * The TransferHandler class responsible for unpacking the received
     * Transferable object. Only drops of (native) files are supported. Checking
     * for image files is done during the transfer.
     */
    private class ImageTransferHandler extends TransferHandler {

        private static final long serialVersionUID = -5841279783952938723L;

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor)
                    || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean importData(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }

            final Transferable t = support.getTransferable();
            URL imageFile = null;
            try {
                final String fileName = (String) t.getTransferData(DataFlavor.stringFlavor);
                imageFile = new URL(fileName);
            } catch (Exception e) {
            }
            try {
                final List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                if (fileList.size() > 0) {
                    imageFile = fileList.get(0).toURI().toURL();
                }
            } catch (Exception e) {
            }
            return setImage(imageFile);
        }
    }
}
