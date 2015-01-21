package cuenen.raymond.image;

import java.awt.BorderLayout;
import java.io.File;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class ImageViewer {

    public static void main(String[] args) throws Exception {
        final URL fileName;
        if (args.length > 0) {
            final File file = new File(args[0]);
            fileName = file.toURI().toURL();
        } else {
            fileName = null;
        }
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            DisplayPanel panel = new DisplayPanel(fileName);
            frame.add(panel);
            frame.validate();
            frame.pack();
            frame.setVisible(true);
        });
    }

    private ImageViewer() {

    }
}
