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

    private static final int DISCONNECT_TIMEOUT = 20000;

    public static BufferedImage getGrayScale(BufferedImage inputImage) {
        BufferedImage img = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = img.getGraphics();
        g.drawImage(inputImage, 0, 0, null);
        g.dispose();
        return img;
    }

    private static void doMain() throws ExecutionException, InterruptedException {

        Map<String, JLabel> faces = new ConcurrentHashMap<>();
        Map<String, Long> lastMessages = new ConcurrentHashMap<>();

        MulticastMessenger messenger = ConfiguredMessenger.INSTANCE;
        String username = System.getProperty("user.name");
        Feed feed = messenger.registerFeed(username, "This is private channel!").get();
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(176, 144));
        webcam.open();

        JFrame frame = new JFrame("Video chat (for deaf-mutes)");

        JFrame.setDefaultLookAndFeelDecorated(true);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 3));
        Container pane = frame.getContentPane();

        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
                    lastMessages.keySet().stream()
                            .filter(topic -> System.currentTimeMillis() - lastMessages.get(topic) > DISCONNECT_TIMEOUT)
                            .forEach(topic -> {
                                JLabel face = faces.get(topic);
                                ImageIcon imageIcon = (ImageIcon) face.getIcon();
                                face.setIcon(new ImageIcon(getGrayScale((BufferedImage) imageIcon.getImage())));
                                lastMessages.remove(topic);
                            });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
                            faces.put(f.getTopic(), picLabel);
                        }
                        if (!lastMessages.containsKey(f.getTopic())) {
                            messenger.subscribe(f, (msg, subscription) -> {
                                lastMessages.put(f.getTopic(), System.currentTimeMillis());
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
