package com.ruoyi.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.common.utils.XueShuStaticParam;
import com.ruoyi.common.utils.DingTalkRobot;
import com.ruoyi.system.domain.turnitin.*;
import com.ruoyi.system.domain.turnitin.res.Paper;
import com.ruoyi.system.domain.turnitin.res.SwsLaunchToken;
import com.ruoyi.system.repository.TurnitinTeacherRepository;
import com.ruoyi.system.service.TurnitinService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service("turnitinService")
public class TurnitinServiceImpl implements TurnitinService {

    @Autowired
    private TurnitinTeacherRepository turnitinAccountService;


    private static final String HOMEPAGE = "https://www.turnitin.com/t_home.asp?login=1&svr=6&lang=en_us";


    @Override
    public boolean submitAssignment(String assignmentId, MultipartFile file) {
        return false;
    }

    @Override
    public String getAiReport(Code code, String paperID) {
        log.info("开始下载AI报告，code is {}, paperID is {}", code, paperID);
        String type = code.getType();
        ManagerAccount managerAccount = null;
        if (!type.equals(XueShuStaticParam.SINGLE_CHECK)) managerAccount = turnitinAccountService.findByAccountType("ai").get(0);
        if (managerAccount == null) {
            log.error("没有找到Turnitin查重账号");
            throw new RuntimeException("错误原因：自助提取不可用，请联系客服");
        }
        // 解析头信息
        Map<String, String> headers = CookieParse.convertCurlToMap(managerAccount.getCurlString());

        //先要获取oid, 再根据oid查询是否已经准备好了,assignment等
        String getOidURL = "https://ev.turnitin.com/paper/" + paperID + "?lang=en_us&cv=1&output=json";
        HttpResponse oidRequest = HttpRequest.get(getOidURL).addHeaders(headers).execute();
        int oidCode = oidRequest.getStatus();
        if (oidCode != 200) {
            log.error("获取作业ID失败，状态码{}：", oidCode);
            throw new RuntimeException("暂未生成AI报告，请检查作业ID是否输入正确，5分钟刷新重试");
        }
        TurnitinSubmissionData turnitinSubmissionData = JSON.parseObject(oidRequest.body(), TurnitinSubmissionData.class);
        String submissionTrn = turnitinSubmissionData.getPapers().get(0).getSubmission_trn();
        String sessionIDUrl = getSessionIDUrl(turnitinSubmissionData, submissionTrn);

        log.info("获取sessionIDUrl成功，开始查询是否准备好了,sessionIDUrl is {}", sessionIDUrl);
        HttpResponse sessionIDResponse = HttpRequest.get(sessionIDUrl).addHeaders(headers).execute();
        if (sessionIDResponse.getStatus() != 200) {
            log.error("获取sessionID失败，状态码{}：{}",  sessionIDResponse.getStatus(),sessionIDResponse.body());
            throw new RuntimeException("AI报告一般需要30分钟左右生成，请提交后30分钟后再查看，超过30分钟联系客服");
        }
        String sessionID = JSON.parseObject(sessionIDResponse.body()).getString("session_token");
        headers.put("authentication", sessionID);
        log.info("获取sessionID成功，开始查询是否准备好了,sessionID is {}", sessionID);
        turnitinJobStatusCheck(submissionTrn, sessionID);
        log.info("AI报告准备好了，开始获取下载链接");

        String downloadName = getAIReportDownloadName(headers, sessionID, submissionTrn, turnitinSubmissionData, paperID);
        log.info("开始获取下载链接，downloadName is {}", downloadName);
        String downloadUrl = getDownloadUrl(downloadName, sessionID);
        code.setLinkedAccount(managerAccount.getAccountName());
        return downloadUrl;
    }

    private static boolean turnitinJobStatusCheck(String submissionTrn, String sessionID) {
        String url = "https://awo-api-usw2.integrity.turnitin.com/submissions/" + submissionTrn + "/ai-writing-report";
        log.info("开始查询AI报告状态，url is {}", url);
        HttpResponse response = HttpRequest.get(url)
                // 设置headers
                .header("Host", "awo-api-usw2.integrity.turnitin.com")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("content-type", "application/json")
                .header("authentication", sessionID)   // ... 其他headers
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("priority", "u=1, i")
                // 注意：这里不需要设置 --compressed，因为Hutool会自动处理gzip等压缩格式
                // 发送请求并获取响应
                .execute();
        int status = response.getStatus();
        if (status != 200) {
            log.error("AI报告暂未COPLETE，状态码{}：" + status);
            throw new RuntimeException("暂未生成AI报告，请5分钟后再试");
        }
        // 获取响应体内容
        String responseBody = response.body();
        JSONObject jsonObject = JSON.parseObject(responseBody);
        // 提取state字段的值
        log.info("AI报告状态：{},status is {}", jsonObject, status);
        String state = jsonObject.getString("state");
        if (state.equals("REJECTED")) {
            throw new RuntimeException("AI目前仅能查英语，且字数在300字到1.5w字之间，请检查文件");
        }
        return true;
    }

    private String getDownloadUrl(String downloadName, String sessionID) {
        String url = "https://sas-api-usw2.platform.turnitin.com/job/" + downloadName;
        Map<String, String> headers = new HashMap<>();
        headers.put("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"");
        headers.put("content-type", "application/json");
        headers.put("authentication", sessionID);
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("accept", "*/*");
        headers.put("origin", "https://awo-usw2.integrity.turnitin.com");
        headers.put("sec-fetch-site", "same-site");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-dest", "empty");
        headers.put("referer", "https://awo-usw2.integrity.turnitin.com/");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("priority", "u=1, i");
        log.info("开始获取下载链接，url is {}", url);
        HttpResponse response = HttpRequest.get(url).addHeaders(headers).execute();
        int code = response.getStatus();
        String body = response.body();
        int tryTimes = 0;
        while (tryTimes <= 5) {
            log.info("获取下载链接，第{}次尝试,code is {},body is {} ", tryTimes, code, body);
            if (code != 200) {
                throw new RuntimeException("获取下载链接失败,请过5分钟后再试");
            }
            tryTimes++;
            JSONObject jsonObject = JSON.parseObject(body);
            if (jsonObject.getBoolean("fileIsReady")) {
                log.info("获取下载链接成功,");
                return jsonObject.getString("url");
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                log.info("获取下载链接失败，休眠200毫秒后重试");
            }
            response = HttpRequest.get(url).addHeaders(headers).execute();
            code = response.getStatus();
            body = response.body();
        }
        throw new RuntimeException("获取下载链接失败，请稍后再试：end");
    }

    private static String getSessionIDUrl(TurnitinSubmissionData turnitinSubmissionData, String submissionTrn) {
        String assignmentId = turnitinSubmissionData.getPapers().get(0).getAssignment();
        //先去获取session-id
        if (submissionTrn == null || submissionTrn.isEmpty() || assignmentId == null || assignmentId.isEmpty()) {
            throw new RuntimeException("submissionTrn或assignmentId为空");
        }
        String[] parts = submissionTrn.split(":");
        String oid = parts[parts.length - 1];
        String sessionIDUrl = "https://ev.turnitin.com/assignment/" + assignmentId + "/session_token?lang=en_us&cv=1&output=json&o=" + oid;
        log.info("sessionIDUrl is {}", sessionIDUrl);
        return sessionIDUrl;
    }

    private String getAIReportDownloadName(Map<String, String> headers, String sessionToken, String submissionTrn, TurnitinSubmissionData turnitinSubmissionData, String paperID) {

        Paper papers = turnitinSubmissionData.getPapers().get(0);
        // 获取jwt
        String legacyAuth = getLegacyAuth(headers, sessionToken, submissionTrn, paperID);
        // 动态params
        Map<String, Object> params = new HashMap<>();
        params.put("author", papers.getAuthor_full_name());
        params.put("submissionTitle", papers.getTitle());
        params.put("timeZone", "Asia/Seoul");

        String url = "https://sas-api-usw2.platform.turnitin.com/job";

        // 构建请求体
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("submissionTrn", submissionTrn);

        Map<String, Object> extension = new HashMap<>();
        extension.put("name", "aiw");

        Map<String, String> config = new HashMap<>();
        config.put("environment", "prod");
        config.put("region", "usw2");
        config.put("sessionToken", sessionToken);

        extension.put("config", config);
        requestBodyMap.put("extensions", new Map[]{extension});

        Map<String, String> mainConfig = new HashMap<>();
        mainConfig.put("environment", "prod");
        mainConfig.put("region", "usw2");
        mainConfig.put("legacyAuth", legacyAuth);
        mainConfig.put("sessionToken", sessionToken);

        requestBodyMap.put("config", mainConfig);
        requestBodyMap.put("params", params);

        String requestBody = JSONUtil.toJsonStr(requestBodyMap);

        // 创建HTTP请求并添加headers
        HttpRequest request = HttpRequest.post(url)
                .header("Host", "sas-api-usw2.platform.turnitin.com")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("content-type", "application/json")
                .header("authentication", sessionToken)
                .header("sec-ch-ua-mobile", "?0")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("Accept", "*/*")
                .header("Origin", "https://awo-usw2.integrity.turnitin.com")
                .header("Sec-Fetch-Site", "same-site")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("Referer", "https://awo-usw2.integrity.turnitin.com/")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .body(requestBody); // 设置请求体

        // 发送请求并获取响应
        HttpResponse response = request.execute();
        if (response.getStatus() != 201) {
            log.error("获取下载链接失败，状态码：" + response.getStatus());
            throw new RuntimeException("获取报告失败，请稍后再试");
        }

        // 解析JSON响应
        String responseBody = response.body();
        log.info("获取下载Mingzi 成功，responseBody is {}", responseBody);
        return responseBody;
    }

    private String getLegacyAuth(Map<String, String> headers, String submissionTrn, String trn, String paperID) {

        return getJWT(headers, submissionTrn, paperID);

    }

    private String getJWT(Map<String, String> headers, String submissionTrn, String paperID) {
        // 获取bear token
        SwsLaunchToken swsLaunchToken = getSwsLaunchToken(headers, submissionTrn, paperID);
        String token = swsLaunchToken.getToken();

        // 获取jwt authen
        String aquraToken = getAquraToken(headers, token);

        // 获取jwt
        log.info("开始获取jwt");
        String url = "https://external-production.us2.turnitin.com/sms-namespace/seu/sms-serviceName/ares/public/jwt";
        HttpRequest request = HttpRequest.get(url)
                .header("Host", "external-production.us2.turnitin.com")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("content-type", "application/json")
                .header("sec-ch-ua-mobile", "?0")
                .header("authorization", "Bearer " + aquraToken)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("Accept", "*/*")
                .header("Origin", "https://ev.turnitin.com")
                .header("Sec-Fetch-Site", "same-site")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("Referer", "https://ev.turnitin.com/")
                .header("Accept-Language", "zh-CN,zh;q=0.9");
        // 发送请求并获取响应
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("获取jwt失败，状态码：" + response.getStatus());
            throw new RuntimeException("获取报告失败，请稍后再试");
        }

        String responseBody = response.body();
        JSONObject jsonObject = JSON.parseObject(responseBody);
        String jwt = jsonObject.getString("access_token");
        log.info("获取jwt成功,jwt is {}", jwt);
        return jwt;
    }

    private String getAquraToken(Map<String, String> headers, String token) {
        log.info("开始获取aquraToken");
        String url = "https://external-production.us2.turnitin.com/aurora/jwt";
        HttpRequest request = HttpRequest.get(url)
                .header("Host", "external-production.us2.turnitin.com")
                .header("sec-ch-ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                .header("content-type", "application/json")
                .header("sec-ch-ua-mobile", "?0")
                .header("authorization", "Bearer " + token)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("Accept", "*/*")
                .header("Origin", "https://ev.turnitin.com")
                .header("Sec-Fetch-Site", "same-site")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .header("Referer", "https://ev.turnitin.com/")
                .header("Accept-Language", "zh-CN,zh;q=0.9");

        // 发送请求并获取响应
        HttpResponse response = request.execute();

        if (response.getStatus() != 200) {
            log.error("获取aquraToken失败，状态码：" + response.getStatus());
            throw new RuntimeException("获取报告失败，请稍后再试");
        }
        String responseBody = response.body();
        log.info("获取aquraToken成功,response is {}", responseBody);
        JSONObject jsonObject = JSON.parseObject(responseBody);
        return jsonObject.getString("access_token");
    }

    private SwsLaunchToken getSwsLaunchToken(Map<String, String> headers, String submissionTrn, String paperID) {

        String url = "https://ev.turnitin.com/paper/" + paperID + "/sws_launch_token?lang=en_us&cv=1&output=json";
        HttpResponse response = HttpUtil.createGet(url).addHeaders(headers).execute();
        if (response.getStatus() != 200) {
            log.error("获取swsLaunchToken失败，状态码：" + response.getStatus());
            throw new RuntimeException("获取报告失败，请稍后再试");
        }
        log.info("获取swsLaunchToken成功,response is {}", response.body());
        return JSON.parseObject(response.body(), SwsLaunchToken.class);
    }

    /**
     * @param url
     * @param headers
     * @return 返回classID, 打开classID的url, name
     */
    private static Optional<List<TurnitinAssignment>> getAssignments(String url, Map<String, String> headers) {
        HttpResponse response = HttpUtil.createGet(url)
                .addHeaders(headers)
                .execute();
        Document doc = Jsoup.parse(response.body());
        Element table = doc.getElementById("class_assignments");
        if (table == null) {
            log.error("No class_assignments table found in " + url);
            return Optional.empty();
        }
        Elements rows = table.select("tbody");
        List<TurnitinAssignment> assignmentList = new ArrayList<>();
        for (Element row : rows) {
            TurnitinAssignment assignmentDetails = new TurnitinAssignment();
            Elements titleRow = row.select("tr.assgn-title");
            String title = titleRow.select("td").text();
            assignmentDetails.setAssignmentTitle(title);
            Elements assignmentRows = row.select("tr.assgn-row");
            for (Element assignmentRow : assignmentRows) {
                String status = assignmentRow.select("td.assgn-status").text();
                String inbox = "https://www.turnitin.com" + assignmentRow.select("td.assgn-inbox a").attr("href");
                String actionsHref = assignmentRow.select("td.assgn-actions a[href*='aid=']").attr("href");
                if (!actionsHref.isEmpty()) {
                    String aid = actionsHref.split("aid=")[1];
                    assignmentDetails.setAssignmentId(aid);
                } else {
                    log.error("No actionsHref" + title + "   " + url);
                }
                assignmentDetails.setAssignmentStatus(status);
                assignmentDetails.setUrl(inbox);
            }
            assignmentList.add(assignmentDetails);
        }
        return Optional.of(assignmentList);
    }


    /**
     * @param headers
     * @param classList
     * @return 班级ID与用户ID的映射关系  比如classID:12345678,email:5lwl3ufdsu6x6p@hotmail.com,userID:123456789
     */
    private Optional<Map<String, Map<String, Long>>> getClassIDToUserIDMap(Map<String, String> headers, List<Map<String, String>> classList) {
        Map<String, Map<String, Long>> classIDToUserIDMap = new HashMap<>();
        for (Map<String, String> stringStringMap : classList) {
            String classID = stringStringMap.get("classID");
            String studentsURL = "https://www.turnitin.com/api/rest/1p0/class/" + classID + "/students?lang=en_us";
            HttpResponse response = HttpUtil.createGet(studentsURL)
                    .addHeaders(headers)
                    .execute();
            int code = response.getStatus();
            if (code != 200) {
                log.error("获取学生列表失败，状态码：{}", code);
                return Optional.empty();
            }
            String body = response.body();
            List<TurnitinStudents> studentsList = JSON.parseArray(body, TurnitinStudents.class);
            Map<String, Long> studentMap = new HashMap<>();
            for (TurnitinStudents student : studentsList) {
                long userID = student.getUser_id();
                String email = student.getEmail();
                studentMap.put(email, userID);
            }
            classIDToUserIDMap.put(classID, studentMap);
        }
        return Optional.of(classIDToUserIDMap);
    }

    /**
     * @param cookies
     * @return 班级列表, 比如classID, 打开classID的url, name
     */
    public static Optional<List<Map<String, String>>> getClasses(Map<String, String> cookies) {
        HttpResponse response = HttpUtil.createGet(HOMEPAGE)
                .addHeaders(cookies)
                .execute();

        return parseDashboard(response.body());
    }

    private static Optional<List<Map<String, String>>> parseDashboard(String source) {
        Document document = Jsoup.parse(source);
        Elements trElements = document.select("tr.class");
        if (trElements == null || trElements.isEmpty()) {
            return Optional.empty();
        }

        List<Map<String, String>> classList = new ArrayList<>();
        for (Element trElement : trElements) {
            Map<String, String> classMap = new HashMap<>();
            // 提取classID
            String classId = trElement.selectFirst(".class_id").text();
            classMap.put("classID", classId);
            // 提取class_name的href
            String classNameHref = trElement.selectFirst(".class_name a").attr("href");
            classMap.put("url", "https://www.turnitin.com/" + classNameHref);
            classList.add(classMap);
        }
        return Optional.of(classList);
    }

    public static Optional<String> extractOidFromJson(String jsonString) {
        // 解析JSON字符串
        JSONObject jsonObject = JSON.parseObject(jsonString);

        // 获取DocumentFlags数组
        JSONArray documentFlagsArray = jsonObject.getJSONArray("DocumentFlags");
        if (documentFlagsArray == null || documentFlagsArray.isEmpty()) {
            return Optional.empty();
        }

        // 获取第一个对象中的submissionId
        String submissionId = documentFlagsArray.getJSONObject(0).getString("submissionId");
        if (submissionId == null || !submissionId.startsWith("oid:")) {
            return Optional.empty();
        }

        // 提取并返回oid值
        String[] parts = submissionId.split(":");
        if (parts.length > 1) {
            return Optional.of(parts[1]);
        }

        return Optional.empty();
    }



    public void expireCodes() {
        log.info("开始维持心跳");
        List<ManagerAccount> managerAccountsList = turnitinAccountService.findAll();
        if (managerAccountsList == null || managerAccountsList.isEmpty()) {
            log.error("没有可用的Turnitin账号");
        }
        for (ManagerAccount managerAccount : managerAccountsList) {
            String curl = managerAccount.getCurlString();
            Map<String, String> params = CookieParse.convertCurlToMap(curl);
            String url = "https://www.turnitin.com/t_home.asp?login=1&svr=6&lang=en_us&r=24.2809311565821";
            HttpRequest request = HttpRequest.get(url).addHeaders(params);
            HttpResponse response = request.execute();
            int code = response.getStatus();
            if (code != 200) {
                log.error("获取Turnitin登录页面失败，状态码：" + code);
                DingTalkRobot.sendMarkdownMsg("Turnitin教师号：" + managerAccount.getAccountName() + " 心跳失败，掉线了 ,获取Turnitin登录页面失败，状态码：" + code);
                throw new RuntimeException("获取Turnitin登录页面失败");
            } else {
                log.info("获取Turnitin登录页面成功");
            }
        }
    }
}
