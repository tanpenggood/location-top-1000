package com.itplh.github.domain;

import lombok.Data;

import java.util.List;

/**
 * @author: tanpenggood
 * @date: 2023-08-05 09:51
 */
@Data
public class Payload {

    private boolean header_redesign_enabled;
    private List<Results> results;
    private String type;
    private int page;
    private int page_count;
    private int elapsed_millis;
    private List<String> errors;
    private int result_count;
    private List<String> protected_org_logins;
    private String topics;
    private String query_id;
    private boolean logged_in;
    private String sign_up_path;
    private String sign_in_path;

}
