package com.itplh.github;

import com.itplh.absengine.util.StringUtils;
import com.itplh.github.domain.Payload;
import com.itplh.github.domain.Results;
import com.itplh.github.domain.SearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
 * @date: 2023-08-05 09:44
 */
@Slf4j
public class GithubLocationUserSearch {

    private Integer totalPages;
    private String location;
    private String targetName;
    private boolean stop = false;

    private GithubLocationUserSearch() {
        // 私有构造方法，防止外部实例化
        location = "chongqing";
        targetName = "tanpenggood";
    }

    public static GithubLocationUserSearch getInstance() {
        return new GithubLocationUserSearch();
    }

    public GithubLocationUserSearch setLocation(String location) {
        this.location = location;
        return this;
    }

    public GithubLocationUserSearch setTargetUser(String username) {
        this.targetName = username;
        return this;
    }


    /**
     * 多线程查询页码范围内的User
     *
     * @param start 开始页(包含)
     * @param end   结束页(包含)
     * @param step  步长
     * @author: tanpenggood
     * @date: 2020/8/24 12:45
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
     * @author: tanpenggood
     * @date: 2020/8/24 12:41
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
     *
     * @author: tanpenggood
     * @date: 2020/8/24 12:41
     */
    public void queryUserByPage(int pageNum) {
        int pageSize = 10;
        SearchResult result = getSearchResult(pageNum);
        Optional.ofNullable(result)
                .map(SearchResult::getPayload)
                .map(Payload::getResults)
                .ifPresent(userList -> {
                    for (int i = 0; i < userList.size(); i++) {
                        Results user = userList.get(i);
                        int no = (pageNum - 1) * pageSize + i + 1;
                        String name = StringUtils.hasText(user.getHl_name()) ? user.getHl_name() : user.getHl_login();
                        String username = user.getHl_login();
                        String githubLink = "https://github.com/" + user.getHl_login();
                        String location = user.getLocation();
                        int repositories = user.getRepos();
                        int followers = user.getFollowers();
                        String profile = user.getHl_profile_bio();
                        Map userInfoMap = new LinkedHashMap() {{
                            put("page", pageNum);
                            put("No", no);
                            put("name", name);
                            put("username", username);
                            put("location", location);
                            put("repositories", repositories);
                            put("followers", followers);
                            put("github-link", githubLink);
                            put("email", "");
                            put("profile", profile);
                        }};

                        StringBuilder infoBuilder = new StringBuilder();
                        userInfoMap.forEach((k, v) -> infoBuilder.append(k).append("[").append(v).append("] "));
                        String info = infoBuilder.toString();
                        if (Objects.equals(this.targetName, name) || Objects.equals(this.targetName, username)) {
                            log.info("\r\n------------------------------------------------------------\r\n{}\r\n------------------------------------------------------------", info);
                            stop = true;
                        } else {
                            log.info(info);
                        }
                    }
                });
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SearchResult getSearchResult(int pageNum) {
        return getSearchResult(pageNum, this.location);
    }

    private SearchResult getSearchResult(int pageNum, String location) {
        pageNum = pageNum < 1 ? 1 : pageNum;
        location = StringUtils.isBlank(location) ? this.location : location;
        String url = String.format("https://github.com/search?p=%s&q=location:%s&type=Users", pageNum, location);
        log.info(url);
        SearchResult result = HttpRequestUtil.getJsonObject(url, SearchResult.class);
        // init totalPages
        if (totalPages == null) {
            totalPages = Optional.ofNullable(result)
                    .map(SearchResult::getPayload)
                    .map(Payload::getPage_count)
                    .orElse(null);
        }
        return result;
    }

}
