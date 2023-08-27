package com.itplh.github.domain;

import lombok.Data;

/**
 * @author: tanpenggood
 * @date: 2023-08-05 09:49
 */
@Data
public class Results {

    private String avatar_url;
    private String hl_login;
    private String hl_name;
    private String hl_profile_bio;
    private boolean followed_by_current_user;
    private int followers;
    private String id;
    private boolean is_current_user;
    private String location;
    private String login;
    private String display_login;
    private String name;
    private String profile_bio;
    private boolean sponsorable;
    private int repos;

}
