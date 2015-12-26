package ru.ifmo.ctddev.isaev;

import ru.zyulyaev.ifmo.net.multicast.api.Feed;
import ru.zyulyaev.ifmo.net.multicast.impl.MulticastMessenger;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

/**
 * @author Ilya Isaev
 */
public class RssReceiver {


    private final Queue<byte[]> incomingMessages;

    private final Map<String, Feed> alreadyDiscovered = new HashMap<>();

    public RssReceiver() {
        MulticastMessenger messenger = ConfiguredMessenger.INSTANCE;
        try {
            while (true) {
                messenger.discoverFeeds().get().forEach(f -> {
                    if (!alreadyDiscovered.containsKey(f.getTopic())) {
                        alreadyDiscovered.put(f.getTopic(), f);
                        try {
                            messenger.subscribe(f, (message, subscription) -> {
                                System.out.println("\nBREAKING NEWS!!! " + new String(message) + "\n");
                            }).get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new RssReceiver();
    }

    public String getNext() {
        return new String(incomingMessages.poll());
    }
}
