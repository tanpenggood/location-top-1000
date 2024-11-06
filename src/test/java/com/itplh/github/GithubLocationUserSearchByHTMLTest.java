package com.itplh.github;

import org.junit.jupiter.api.Test;

class GithubLocationUserSearchByHTMLTest {

    @Test
    void multithreadingQueryUserByPageRange() {
        GithubLocationUserSearchByHTML.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .multithreadingQueryUserByPageRange(58, 63, 2);
    }

    @Test
    void queryUserByPageRange() {
        GithubLocationUserSearchByHTML.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .queryUserByPageRange(59, 61);
    }

    @Test
    void queryUserByPage() {
        GithubLocationUserSearchByHTML.getInstance()
                .setTargetUser("tanpenggood")
                .setLocation("chongqing")
                .queryUserByPage(60);
    }

}