package com.ruoyi.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.common.utils.SentryTraceGenerator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class MultipartFormDataExample {

    public static void main(String[] args) {
        String url = "https://scopedlens.com/self-service/submission/create";
        String boundary = "----WebKitFormBoundaryLULnqKyqKxsDrOaO";  // 使用指定的 boundary

        String curl = "curl 'https://scopedlens.com/self-service/submissions/?partial=true&page=1' \\   -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7' \\   -H 'accept-language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \\   -H 'cookie: csrftoken=DhJpUF8zIeD0JA5FCLRl29LW1Hbs3Obt; sessionid=l61hsj3iz04ppq4lgq03oi176u4no2qw; arp_scroll_position=225' \\   -H 'dnt: 1' \\   -H 'priority: u=0, i' \\   -H 'sec-ch-ua: \"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Microsoft Edge\";v=\"126\"' \\   -H 'sec-ch-ua-mobile: ?0' \\   -H 'sec-ch-ua-platform: \"macOS\"' \\   -H 'sec-fetch-dest: document' \\   -H 'sec-fetch-mode: navigate' \\   -H 'sec-fetch-site: none' \\   -H 'sec-fetch-user: ?1' \\   -H 'upgrade-insecure-requests: 1' \\   -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0'";


        Map<String, String> getHeaders = CookieParse.convertCurlToMap(curl);


        String csrfmiddlewaretoken = getCsrfToken(getHeaders);

        String traceId = SentryTraceGenerator.generateTraceId();
        String spanId = SentryTraceGenerator.generateSpanId();
        Map<String, String> headers = getEntryTracy(traceId, spanId,getHeaders);
        // 添加文件字段
        File file = new File("/Users/huwenjia/Downloads/版权声明函 (1).docx");

        // 设置请求参数
        Map<String, Object> formParams = new HashMap<>();
        formParams.put("csrfmiddlewaretoken", csrfmiddlewaretoken);
        formParams.put("region", "intl");
        formParams.put("upload_document", file); // 请修改为实际文件路径
        formParams.put("title", file.getName());
        formParams.put("exclude_small_matches_method", "disabled");
        formParams.put("exclude_small_matches_value_words", "0");
        formParams.put("exclude_small_matches_value_percentage", "0");
        // 发送POST请求
        HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .form(formParams)
                .execute();

        if (response.getStatus() != 302) {
            System.out.println("上 传文件失败"+response.body());
        }
    }

    private static String getCsrfToken(Map<String, String> originhHeaders) {
        String url = "https://scopedlens.com/self-service/submission/create";

        // 发送请求并获取响应
        HttpResponse response = HttpRequest.get(url)
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("cookie", originhHeaders.get("cookie"))
                .header("dnt", "1")
                .header("priority", "u=0, i")
                .header("referer", "https://scopedlens.com/self-service/submissions/")
                .header("sec-ch-ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Microsoft Edge\";v=\"126\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "document")
                .header("sec-fetch-mode", "navigate")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-user", "?1")
                .header("upgrade-insecure-requests", "1")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                .execute();

        // 打印响应状态码和响应体

        if (response.getStatus() != 200) {
            throw new RuntimeException("获取csrftoken失败，请联系人工客服");
        }
        Document document = Jsoup.parse(response.body());
        Element csrfElement = document.select("input[name=csrfmiddlewaretoken]").first();
        if (csrfElement != null) {
            return csrfElement.attr("value");
        }
        return null;
    }

    public static Map<String, String> getEntryTracy(String traceId, String spanId, Map<String, String> getHeaders) {
        boolean sampled = true; // or false, depending on your sampling logic

        String sentryTrace = SentryTraceGenerator.generateSentryTrace(traceId, spanId, sampled);

        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "scopedlens.com");
        headers.put("Cookie", getHeaders.get("cookie"));
        headers.put("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
        headers.put("content-type", "multipart/form-data; boundary=----WebKitFormBoundaryo7g3sbovVhgCdmcP");
        headers.put("baggage", getHeaders.get("baggage"));
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        headers.put("sentry-trace", sentryTrace);
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("accept", "*/*");
        headers.put("origin", "https://scopedlens.com");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-dest", "empty");
        headers.put("referer", "https://scopedlens.com/self-service/submission/create");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("priority", "u=1, i");
        return headers;
    }
}
