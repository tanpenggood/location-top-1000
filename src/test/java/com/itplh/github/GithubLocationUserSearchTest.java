package com.itplh.github;

import org.junit.jupiter.api.Test;

class GithubLocationUserSearchTest {

    @Test
    void multithreadingQueryUserByPageRange() {
        GithubLocationUserSearch.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .multithreadingQueryUserByPageRange(58, 63, 2);
    }

    @Test
    void queryUserByPageRange() {
        GithubLocationUserSearch.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .queryUserByPageRange(59, 61);
    }

    @Test
    void queryUserByPage() {
        GithubLocationUserSearch.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .queryUserByPage(60);
    }

}