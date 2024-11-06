package com.itplh.github;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: tanpenggood
 * @date: 2023-11-06 23:15
 */
@Slf4j
public class GithubLocationUserSearchByHTML {

    private static HashMap<String, String> headers = new HashMap<>();

    static {
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "_octo=GH1.1.1078431350.1597553477; _ga=GA1.2.794663729.1597553486; _device_id=42a018ebe586fcf588025f3a25baf176; user_session=9xjtxVCnR4xrzb6nYnURdOvPzrGZww9-Zo2BbCG_NKK5TTpF; __Host-user_session_same_site=9xjtxVCnR4xrzb6nYnURdOvPzrGZww9-Zo2BbCG_NKK5TTpF; logged_in=yes; dotcom_user=tanpenggood; has_recent_activity=1; _gat=1; tz=Asia%2FShanghai; _gh_sess=HnHuDpyMPdN8j7i%2BDjoh2fqgFYRzIvdLTRfCjbzwjVC2Bkwm8qda8rEPa4ilerURrbljLWsXSUVPCPXAKh6xYroR9aZctisqLoVRgA908NbGDym36NaRMsimNupbWCu20QKpDFHHU7adSbQ1w0eh1nGJES20zlAFlcPpFqCqrY76Wzbb%2BPIAMWC8eJGiIL1m--GIimUrbITnIkjFQu--uaHFCERmVfQgqXhQ1cPK2A%3D%3D");
        headers.put("Host", "github.com");
        headers.put("If-None-Match", "W/\"83a70ffeef4b4593de8500fec8ca6cbe\"");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36");
    }

    private Integer totalPages;
    private String location;
    private String targetName;
    private boolean stop = false;

    private GithubLocationUserSearchByHTML() {
        // 私有构造方法，防止外部实例化
        location = "chongqing";
        targetName = "tanpenggood";
    }

    public static GithubLocationUserSearchByHTML getInstance() {
        return new GithubLocationUserSearchByHTML();
    }

    public GithubLocationUserSearchByHTML setLocation(String location) {
        this.location = location;
        return this;
    }

    public GithubLocationUserSearchByHTML setTargetUser(String username) {
        this.targetName = username;
        return this;
    }

    /**
     * 多线程查询页码范围内的User
     *
     * @param start 开始页(包含)
     * @param end   结束页(包含)
     * @param step  步长
     */
    public void multithreadingQueryUserByPageRange(int start, int end, int step) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("处理器个数为{}", availableProcessors);
        ExecutorService threadPool = Executors.newFixedThreadPool(availableProcessors);
        List<CompletableFuture> tasks = new ArrayList<>();
        do {
            int startPageNum = start;
            int endPageNum = (start + step - 1) > end ? end : (start + step - 1);
            log.info("page:{}~{}", startPageNum, endPageNum);
            tasks.add(CompletableFuture.runAsync(() -> queryUserByPageRange(startPageNum, endPageNum), threadPool));
            start += step;
        } while (start <= end);
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        threadPool.shutdown();
    }

    /**
     * 查询页码范围内的User
     *
     * @param start 开始页(包含)
     * @param end   结束页(包含)
     */
    public void queryUserByPageRange(int start, int end) {
        int currentPageNum = start < 1 ? 1 : start;
        do {
            if (stop) {
                log.warn("early termination of query, stop=true");
                return;
            }
            queryUserByPage(currentPageNum);
            end = end > totalPages ? totalPages : end;
            currentPageNum++;
        } while (currentPageNum <= end);
    }

    /**
     * 查询指定页的User
     */
    public void queryUserByPage(int pageNum) {
        Document document = getDocument(pageNum);
        Optional.ofNullable(document)
                .map(doc -> doc.getElementsByAttributeValue("data-testid", "results-list"))
                .map(Elements::first)
                .map(Element::children)
                .ifPresent(userList -> {
                    for (int i = 0; i < userList.size(); i++) {
                        Element user = userList.get(i);
                        String no = (pageNum - 1) * 10 + i + 1 + "";
                        Map userInfoMap = new LinkedHashMap() {{
                            put("page", pageNum);
                            put("No", no);
                            put("name", "");
                            put("username", "");
                            put("github-link", "");
                            put("email", "");
                            put("location", "");
                            put("repositories", "");
                            put("followers", "");
                            put("profile", "");
                        }};
                        // name username github-link
                        Optional.ofNullable(user)
                                .map(u -> u.getElementsByClass("search-title"))
                                .map(Elements::last)
                                .ifPresent(element -> {
                                    // name
                                    userInfoMap.put("name", Optional.ofNullable(element).map(d -> d.getElementsByTag("span")).map(Elements::first).map(Element::text).orElse(""));
                                    // username
                                    userInfoMap.put("username", Optional.ofNullable(element).map(d -> d.getElementsByTag("span")).map(Elements::last).map(Element::text).orElse(""));
                                    // github-link
                                    Optional.ofNullable(element).map(d -> d.getElementsByTag("a")).map(Elements::last).map(d -> d.attr("href")).ifPresent(githubLink -> {
                                        userInfoMap.put("github-link", "https://github.com" + githubLink);
                                    });
                                });
                        // location
                        Optional.ofNullable(user)
                                .map(u -> u.getElementsByTag("li"))
                                .ifPresent(lis -> {
                                    userInfoMap.put("location", Optional.ofNullable(lis).map(Elements::first).map(d -> d.getElementsByTag("span")).map(Elements::first).map(Element::text).orElse(""));
                                    userInfoMap.put("repositories", Optional.ofNullable(lis).map(eles -> eles.get(1)).map(d -> d.getElementsByTag("span")).map(Elements::first).map(Element::text).orElse(""));
                                    userInfoMap.put("followers", Optional.ofNullable(lis).map(Elements::last).map(d -> d.getElementsByTag("span")).map(Elements::first).map(Element::text).orElse(""));
                                });
                        // profile
                        Optional.ofNullable(user)
                                .map(u -> u.getElementsByClass("search-match"))
                                .map(Elements::last)
                                .map(Element::text)
                                .ifPresent(profile -> userInfoMap.put("profile", profile));

                        StringBuilder infoBuilder = new StringBuilder();
                        userInfoMap.forEach((k, v) -> infoBuilder.append(k).append("[").append(v).append("] "));
                        String info = infoBuilder.toString();
                        if (Objects.equals(targetName, userInfoMap.get("name")) || Objects.equals(targetName, userInfoMap.get("username"))) {
                            log.info("\r\n------------------------------------------------------------\r\n{}\r\n------------------------------------------------------------", info);
                            stop = true;
                        } else {
                            log.info(info);
                        }
                    }
                });
        try {
            TimeUnit.MILLISECONDS.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送http请求获取Document
     * 失败自动重试一次
     *
     * @param pageNum 页码
     */
    private Document getDocument(long pageNum) {
        String location = Optional.ofNullable(this.location).orElse("chongqing");
        pageNum = pageNum < 1 ? 1 : pageNum;
        String url = String.format("https://github.com/search?p=%s&q=location:%s&type=users", pageNum, location);
        log.info(url);
        Document document = null;
        try {
            document = Jsoup.connect(url).headers(headers).timeout(15000).get();
        } catch (Exception e) {
            try {
                log.error("page {}，timeout...", pageNum);
                document = Jsoup.connect(url).headers(headers).timeout(15000).get();
            } catch (Exception e1) {
                log.error("page {}，request failed...", pageNum);
            }
        }
        // init totalPages
        if (totalPages == null) {
            Optional.ofNullable(document)
                    .map(d -> d.getElementsByAttributeValue("rel", "next"))
                    .map(Elements::first)
                    .map(Element::previousElementSibling)
                    .map(Element::text)
                    .map(Integer::parseInt)
                    .ifPresent(totalPages -> {
                        this.totalPages = totalPages;
                        log.info("total pages={}", this.totalPages);
                    });
        }
        return document;
    }

}
