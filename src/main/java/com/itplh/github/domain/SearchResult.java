package com.itplh.github.domain;

import lombok.Data;

/**
 * @author: tanpenggood
 * @date: 2023-08-05 09:48
 */
@Data
public class SearchResult {

    private Payload payload;
    private String title;
    private String locale;

}
