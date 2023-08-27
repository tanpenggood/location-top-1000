package com.itplh.github;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.itplh.absengine.util.AssertUtils;
import com.itplh.absengine.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: tanpenggood
 * @date: 2023-08-05 09:15
 */
@Slf4j
public class HttpRequestUtil {

    public static String getJsonString(String link) {
        return getJsonStringPlus(link, Collections.EMPTY_MAP);
    }

    public static <T> T getJsonObject(String link, Map<String, String> headers, Class<T> targetClass) {
        String json = getJsonStringPlus(link, headers);
        return JSONObject.parseObject(json, targetClass);
    }

    public static <T> T getJsonObject(String link, Class<T> targetClass) {
        String json = getJsonString(link);
        return JSONObject.parseObject(json, targetClass);
    }

    public static JSONObject getJsonObject(String link, Map<String, String> headers) {
        String json = getJsonStringPlus(link, headers);
        return JSONObject.parseObject(json);
    }

    public static JSONObject getJsonObject(String link) {
        String json = getJsonString(link);
        return JSONObject.parseObject(json);
    }

    private static String getJsonStringPlus(String link, Map<String, String> headers) {
        try {
            return getJsonString(link, headers);
        } catch (Throwable e) {
            log.error("{}", e.getMessage());
            try {
                TimeUnit.SECONDS.sleep(1);
                return getJsonString(link, headers);
            } catch (Throwable ex) {
                log.error("{}", ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
    }

    private static String getJsonString(String link, Map<String, String> headers) {
        AssertUtils.assertNotBlank(link, "link is required.");
        InputStreamReader reader = null;
        BufferedReader in = null;
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // headers
            if (CollectionUtils.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            in = new BufferedReader(reader);
            String line = null;
            StringBuffer content = new StringBuffer();
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (SocketTimeoutException e) {
            log.error("connection time out.");
            throw new RuntimeException(e);
        } catch (JSONException e) {
            log.error("json convert exception.");
            throw new RuntimeException(e);
        } catch (Throwable e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

}
