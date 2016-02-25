package ru.ifmo.ctddev.isaev;

import ru.zyulyaev.ifmo.net.multicast.impl.MulticastMessenger;

import java.io.IOException;

/**
 * @author Ilya Isaev
 */
public class ConfiguredMessenger {
    public static final MulticastMessenger INSTANCE;

    private static final int PORT = 4000;

    static {
        try {
            INSTANCE = MulticastMessenger.open(PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
