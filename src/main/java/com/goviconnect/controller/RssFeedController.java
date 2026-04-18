package com.goviconnect.controller;

import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api")
public class RssFeedController {

    // Using Google News RSS as a "brute force" proxy to get AgWeb content without 403 blocks
    private static final String RSS_URL = "https://news.google.com/rss/search?q=site:agweb.com+crops&hl=en-US&gl=US&ceid=US:en";
    private static final String FALLBACK_IMAGE = "/images/global-news-ref.png";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    private final CopyOnWriteArrayList<Map<String, String>> cachedNews = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        refreshFeed();
    }

    @GetMapping("/rss-news")
    public ResponseEntity<List<Map<String, String>>> getRssNews() {
        if (cachedNews.isEmpty()) {
            refreshFeed();
        }
        return ResponseEntity.ok(Collections.unmodifiableList(cachedNews));
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void refreshFeed() {
        try {
            logDebug("Refreshing feed from Google News proxy: " + RSS_URL);
            List<Map<String, String>> items = fetchAndParseFeed();
            if (!items.isEmpty()) {
                cachedNews.clear();
                cachedNews.addAll(items);
                logDebug("Successfully loaded " + items.size() + " items from Google News.");
            } else {
                logDebug("Warning: Parsed 0 items from RSS.");
            }
        } catch (Exception e) {
            logDebug("Error refreshing RSS feed: " + e.getMessage());
        }
    }

    private void logDebug(String msg) {
        try (java.io.FileWriter fw = new java.io.FileWriter("rss_controller.log", true);
             java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
            pw.println(new java.util.Date() + " : " + msg);
        } catch (Exception ignored) {}
    }

    private List<Map<String, String>> fetchAndParseFeed() throws Exception {
        org.jsoup.Connection.Response response = Jsoup.connect(RSS_URL)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .ignoreContentType(true)
                .execute();

        Document doc = response.parse();
        Elements itemNodes = doc.select("item");
        List<Map<String, String>> result = new ArrayList<>();

        int limit = Math.min(itemNodes.size(), 12);
        for (int i = 0; i < limit; i++) {
            Element item = itemNodes.get(i);

            String title       = item.select("title").text();
            String link        = item.select("link").text();
            String description = stripHtml(item.select("description").text());
            String pubDate     = item.select("pubDate").text();
            String image       = getPremiumImage(title);

            // Google News titles often end with " - AgWeb", let's clean it up for the UI
            if (title.endsWith(" - AgWeb")) {
                title = title.substring(0, title.length() - 8);
            }

            result.add(Map.of(
                "title",       title != null ? title : "",
                "link",        link != null && !link.isEmpty() ? link : "#",
                "description", description != null ? description : "",
                "pubDate",     pubDate != null ? pubDate : "",
                "image",       image
            ));
        }
        return result;
    }

    private String getPremiumImage(String title) {
        // User requested to use the reference image for all news feed items
        return FALLBACK_IMAGE;
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        // Google News description often contains HTML links and font tags
        return Jsoup.parse(html).text().replaceAll("\\s{2,}", " ").trim();
    }
}
