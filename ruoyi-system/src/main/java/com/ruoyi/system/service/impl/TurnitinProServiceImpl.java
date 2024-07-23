package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.common.utils.DingTalkRobot;
import com.ruoyi.common.utils.SentryTraceGenerator;
import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.ManagerAccount;
import com.ruoyi.system.repository.CodeRepository;
import com.ruoyi.system.service.TurnitinAccountService;
import com.ruoyi.system.service.TurnitinProService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service("turnitinProService")
@Slf4j
public class TurnitinProServiceImpl implements TurnitinProService {


    private static final String TURNITIN_PRO_BASE_URL = "https://scopedlens.com";

    @Autowired
    private TurnitinAccountService turnitinAccountService;
    @Autowired
    private CodeRepository codeRepository;


    @Override
    public boolean submitFile(Code code, String title, String region, MultipartFile file) {
        List<ManagerAccount> turnitinProAccountList = turnitinAccountService.getAccountByAccountType("pro");
        if (turnitinProAccountList.size() == 0) {
            log.error("提交失败，没有找到Turnitin Pro账号");
            throw new RuntimeException("查重目前不可用，请联系人工客服");
        }
        Collections.shuffle(turnitinProAccountList);
        ManagerAccount account = turnitinProAccountList.get(0);
        String curl = account.getCurlString();
        if (!submitForm(code, region, curl, "disabled", 0, 0, file)) {
            log.error("提交失败，账号是：{}", account.getAccountName());
            throw new RuntimeException("提交失败，请联系人工客服");
        }
        code.setLinkedAccount(account.getAccountName());
        if (code.getUsedCount() == null) {
            code.setUsedCount(1);
        } else {
            code.setUsedCount(code.getUsedCount() + 1);
        }
        if (code.getActiveDate() == null) code.setActiveDate(new Date());
        code.setStatus(XueShuStaticParam.USED);
        codeRepository.save(code);
        return true;
    }


    @Override
    public Code getReport(Code code) {
        List<ManagerAccount> turnitinProAccountList = turnitinAccountService.getAccountByAccountType("pro");
        if (CollectionUtil.isEmpty(turnitinProAccountList)) {
            log.error("查重失败，没有找到Turnitin Pro账号");
            throw new RuntimeException("查重目前不可用，请联系人工客服");
        }
        String teacherAccount = code.getLinkedAccount();
        String curl = null;
        for (ManagerAccount managerAccount : turnitinProAccountList) {
            if (managerAccount.getAccountName().equals(teacherAccount)) {
                curl = managerAccount.getCurlString();
                break;
            }
        }
        if (curl == null) {
            log.error("没有找到对应的账号");
            throw new RuntimeException("Pro账号已失效，请联系人工客服");
        }
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String url = "https://scopedlens.com/self-service/submissions/";

        HttpRequest request = HttpUtil.createGet(url)
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("cache-control", "no-cache")
                .header("cookie", headers.get("cookie"))
                .header("pragma", "no-cache")
                .header("priority", "u=0, i")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "document")
                .header("sec-fetch-mode", "navigate")
                .header("sec-fetch-site", "none")
                .header("sec-fetch-user", "?1")
                .header("upgrade-insecure-requests", "1")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");

        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("获取turnitinPro状态失败，原因是：{}", response.body());
            throw new RuntimeException("获取turnitinPro状态失败，请联系人工客服");
        }
        Document document = Jsoup.parse(response.body());

        // 获取表格中的行
        Elements rows = document.select(".submission-row");
        // 遍历每一行并提取数据
        boolean findPaper = false;

        for (Element row : rows) {
            String title = row.select("td a.btn").first().text();
            if (!compareStrings(title, code.getFileName())) {
                continue;
            }
            findPaper = true;
            // 提取状态
            String status = row.select("td:nth-child(5)").text();
            if (status.equals("FAILED") || status.equals("FAILURE")) {
                throw new RuntimeException("获取结果失败，该文件不可查重，请联系人工客服");
            }
            if (!status.equals("SUCCESS")) {
                throw new RuntimeException("查重报告暂未生成，请5分钟后重试");
            }
            // 提取字数
            String wordCount = row.select("td:nth-child(2)").text();
            code.setWordCount(Integer.valueOf(wordCount));
            // 提取相似度
            String similarity = row.select("td:nth-child(3)").first().text().trim();
            code.setSimilarity(similarity);
            String similarityPdfUrl = row.select("td:nth-child(3) a.btn-outline-danger").attr("href");
            code.setSimilarityPdfUrl(similarityPdfUrl);
            // 提取AI写作及其PDF下载链接
            String aiWriting = row.select("td:nth-child(4)").first().text().trim();
            code.setAiWriting(aiWriting);
            String aiWritingPdfUrl = row.select("td:nth-child(4) a.btn-outline-info").attr("href");
            code.setAiWritingPdfUrl(aiWritingPdfUrl);

            // 提取删除按钮的相关信息
            Element deleteButton = row.select("td:nth-child(7) button.delete-item").first();
            String deleteUrl = deleteButton.attr("hx-delete");
            code.setDeleteUrl(TURNITIN_PRO_BASE_URL + deleteUrl);
            // update code
            if (code.getUsedCount() != null && code.getUsageLimit() != null && code.getUsedCount() >= code.getUsageLimit()) {
                code.setStatus(XueShuStaticParam.EXPIRED);
            }
            codeRepository.save(code);
        }
        if (!findPaper) {
            log.error("报告不存在或已删，title：{}", code.getFileName());
            throw new RuntimeException("报告不存在或已删除");
        }
        return code;
    }


    private boolean compareStrings(String str1, String str2) {
        // 去掉字符串两端的空格，并移除中间的所有空格
        str1 = str1.trim().replaceAll("\\s+", "");
        str2 = str2.trim().replaceAll("\\s+", "");

        // 如果第一个字符串包含省略号，截取省略号之前的部分
        int ellipsisIndex = str1.indexOf("...");
        if (ellipsisIndex != -1) {
            str1 = str1.substring(0, ellipsisIndex);
        }

        // 去掉第一个字符串的文件扩展名
        int dotIndex1 = str1.lastIndexOf('.');
        if (dotIndex1 != -1) {
            str1 = str1.substring(0, dotIndex1);
        }

        // 判断两个字符串是否吻合
        return str2.contains(str1);
    }

    @Override
    public void deleteReport(Code code) {
        String deleteUrl = code.getDeleteUrl();
        if (deleteUrl == null){
            log.error("该报告已删除，无需再次删除");
            throw new RuntimeException("该报告已删除，无需再次删除");
        }
        String accountName = code.getLinkedAccount();
        ManagerAccount account = turnitinAccountService.getTurnitinProAccount(accountName).orElseThrow(() -> new RuntimeException("删除失败，该Pro" + accountName + "账号不存在,联系客服删除"));
        String curl = account.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String traceId = SentryTraceGenerator.generateTraceId();
        String spanId = SentryTraceGenerator.generateSpanId();
        String sentryTrace = SentryTraceGenerator.generateSentryTrace(traceId, spanId, true);

        HttpResponse response = HttpRequest.delete(deleteUrl)
                .header("Host", "scopedlens.com")
                .header("Cookie", headers.get("cookie"))
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("hx-current-url", "https://scopedlens.com/self-service/submissions/")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("content-type", "application/x-www-form-urlencoded")
                .header("hx-request", "true")
                .header("baggage", headers.get("baggage"))
                .header("sentry-trace", sentryTrace)
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("accept", "*/*")
                .header("origin", "https://scopedlens.com")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("referer", "https://scopedlens.com/self-service/submissions/")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("priority", "u=1, i")
                .execute();
        if (response.getStatus() != 200) {
            log.error("删除失败，原因是：{}", response.body());
            throw new RuntimeException("删除失败，请联系人工客服");
        }
        code.setDeleteUrl(null);
        code.setWordCount(null);
        code.setSimilarity(null);
        code.setAiWriting(null);
        code.setAiWritingPdfUrl(null);
        code.setSimilarityPdfUrl(null);
        codeRepository.save(code);
    }


    public void autoFreshTurnitinPro() {
        System.out.printf(";;;;;;;开始自动刷新Turnitin Pro账号;;;;;;;;");
       List<ManagerAccount> turnitinProAccountList = turnitinAccountService.getAccountByAccountType("pro");
       log.info("开始自动刷新Turnitin Pro账号");
       for (ManagerAccount managerAccount : turnitinProAccountList) {
           String curl = managerAccount.getCurlString();
           Map<String, String> headers = CookieParse.convertCurlToMap(curl);
           String url = "https://scopedlens.com/self-service/submissions/";

           HttpRequest request = HttpUtil.createGet(url).addHeaders(headers);
           HttpResponse response = request.execute();
           if (response.getStatus() != 200) {
               log.error("获取turnitinPro状态失败，账号是：{},原因是：{}", managerAccount.getAccountName(), response.body());
               DingTalkRobot.sendMsg("获取turnitinPro状态失败，账号是：" + managerAccount.getAccountName() + "，原因是：" + response.body());
           }
       }
    }

    private Boolean submitForm(Code currentCode,String region, String curl,  String excludeSmallMatchesMethod, int excludeSmallMatchesValueWords, int excludeSmallMatchesValuePercentage, MultipartFile file) {
        File convFile;
        try {
            convFile =CookieParse.convertToFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> getHeaders = CookieParse.convertCurlToMap(curl);
        String csrfmiddlewaretoken = getCsrfToken(getHeaders);
        log.info("csrftoken is {}", csrfmiddlewaretoken);

        if (csrfmiddlewaretoken == null) {
            log.error("获取csrftoken失败");
            throw new RuntimeException("获取csrftoken为空，请联系人工客服");
        }
        String url = "https://scopedlens.com/self-service/submission/create";

        String traceId = SentryTraceGenerator.generateTraceId();
        String spanId = SentryTraceGenerator.generateSpanId();
        Map<String, String> headers = getEntryTracy(traceId, spanId,getHeaders);

        // 设置请求参数
        Map<String, Object> formParams = new HashMap<>();
        formParams.put("csrfmiddlewaretoken", csrfmiddlewaretoken);
        formParams.put("region", region);
        formParams.put("upload_document", convFile); // 请修改为实际文件路径
        formParams.put("title", convFile.getName());
        formParams.put("exclude_small_matches_method", "disabled");
        formParams.put("exclude_small_matches_value_words", excludeSmallMatchesValueWords);
        formParams.put("exclude_small_matches_value_percentage", excludeSmallMatchesValuePercentage);
        // 发送POST请求
        HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .form(formParams)
                .execute();
        int status = response.getStatus();
        if (status != 302) {
            // 删除临时文件
            convFile.delete();
            log.error("提交失败，原因是：{}", response);
            throw new RuntimeException("提交查重失败，请重试");
        }
        currentCode.setFileName(convFile.getName());
        convFile.delete();
        return true;
    }

    public static Map<String, String> getEntryTracy(String traceId, String spanId, Map<String, String> getHeaders) {
        boolean sampled = true; // or false, depending on your sampling logic

        String sentryTrace = SentryTraceGenerator.generateSentryTrace(traceId, spanId, sampled);

        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "scopedlens.com");
        headers.put("Cookie", getHeaders.get("cookie"));
        headers.put("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
        headers.put("content-type", "multipart/form-data; boundary=----WebKitFormBoundaryo7g3sbovVhgCdmcP");
        headers.put("baggage", "sentry-environment=production,sentry-public_key=3dd2352722bbe1fcdd4c5d4a4c115a0d,sentry-trace_id="+traceId);
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

    private String getCsrfToken(Map<String, String> originhHeaders) {
        String url = "https://scopedlens.com/self-service/submission/create";
        // 设置请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "scopedlens.com");
        headers.put("Cookie", originhHeaders.get("cookie"));
        headers.put("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("upgrade-insecure-requests", "1");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-mode", "navigate");
        headers.put("sec-fetch-user", "?1");
        headers.put("sec-fetch-dest", "document");
        headers.put("referer", "https://scopedlens.com/self-service/submissions/");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("priority", "u=0, i");

        log.info("headers is {}", headers);

        // 创建HTTP请求
        HttpRequest request = HttpRequest.get(url);
        // 设置头部信息
        headers.forEach(request::header);

        // 发送请求并获取响应
        HttpResponse response = request.execute();

        if (response.getStatus() != 200) {
            log.error("获取csrftoken失败，原因是：{}", response);
            throw new RuntimeException("获取csrftoken失败，请联系人工客服");
        }
        Document document = Jsoup.parse(response.body());
        Element csrfElement = document.select("input[name=csrfmiddlewaretoken]").first();
        if (csrfElement != null) {
            return csrfElement.attr("value");
        }
        return null;
    }

}