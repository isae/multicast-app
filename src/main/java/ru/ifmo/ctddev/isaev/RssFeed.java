package ru.ifmo.ctddev.isaev;

import ru.zyulyaev.ifmo.net.multicast.api.Feed;

/**
 * @author Ilya Isaev
 */
public class RssFeed {
    private Feed feed;
    private String url;

    public RssFeed(Feed feed, String url) {
        this.feed = feed;
        this.url = url;
    }

    public Feed getFeed() {
        return feed;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "RssFeed{" +
                "feed=" + feed +
                ", url='" + url + '\'' +
                '}';
    }
}
