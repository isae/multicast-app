package ru.ifmo.ctddev.isaev;

import com.github.sarxos.webcam.Webcam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zyulyaev.ifmo.net.multicast.api.Feed;
import ru.zyulyaev.ifmo.net.multicast.impl.MulticastMessenger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;


/**
 * Created by iisaev on 25/02/16.
 */
public class WebcamStreamer {
    private static final Logger logger = LoggerFactory.getLogger(WebcamStreamer.class);

    private static int latency;

    private static void doMain() throws ExecutionException, InterruptedException {

        Map<String, JLabel> faces = new ConcurrentHashMap<>();

        MulticastMessenger messenger = ConfiguredMessenger.INSTANCE;
        String username = System.getProperty("user.name");
        Feed feed = messenger.registerFeed(username, "This is private channel!").get();
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(176, 144));
        webcam.open();

        JFrame frame = new JFrame("Video chat (for deaf-mutes)");

        JFrame.setDefaultLookAndFeelDecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 3));
        Container pane = frame.getContentPane();

        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        new Thread(() -> {
            while (true) {
                BufferedImage image = webcam.getImage();
                try {
                    if (image != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);
                        byte[] bytes = baos.toByteArray();
                        messenger.sendMessage(feed, bytes);
                        logger.info("Message sent!");
                        Thread.sleep(latency);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start(); // streamer


        new Thread(() -> {

            while (true) {
                try {
                    messenger.discoverFeeds().get().forEach(f -> {
                        if (!faces.containsKey(f.getTopic())) {
                            JLabel picLabel = new JLabel();
                            picLabel.setText("<html><b>" + f.getTopic() + "</b></html>");
                            picLabel.setHorizontalTextPosition(JLabel.CENTER);
                            picLabel.setVerticalTextPosition(JLabel.BOTTOM);
                            pane.add(picLabel);
                            frame.validate();
                            frame.repaint();
                            frame.pack();
                            faces.put(f.getTopic(), picLabel);
                            messenger.subscribe(f, (msg, subscription) -> {
                                logger.info("Message received! from " + f.getTopic());
                                JLabel face = faces.get(f.getTopic());
                                try {
                                    face.setIcon(new ImageIcon(ImageIO.read(new ByteArrayInputStream(msg))));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    });
                    Thread.sleep(5000);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start(); // receiver
    }

    public static void main(String[] args) throws Exception {

        latency = Integer.valueOf(args[0]);

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                doMain();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
