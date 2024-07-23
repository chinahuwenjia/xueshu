package com.ruoyi.system.service.token;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.system.domain.turnitin.Code;
import com.ruoyi.system.domain.turnitin.GrammarlyDocumentRes;
import com.ruoyi.system.domain.turnitin.ManagerAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class GrammarlyOAuth2Client {
    private static final String AUTHORIZATION_URL = "https://tokens.grammarly.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://tokens.grammarly.com/oauth2/token";
    private static final String UPLOAD_URL = "https://dox.grammarly.com/documents?enableRTF=false";
    private static final String CLIENT_ID = "webeditor_chrome";
    private static final String REDIRECT_URI = "https://app.grammarly.com";
    private static final String SCOPE = "grammarly.capi.all";


    public static GrammarlyDocumentRes uploadFileToGrammarly(Code currentCode, MultipartFile file, ManagerAccount managerAccount) {

        String curl = managerAccount.getCurlString();
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String state = generateState();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge;
        try {
            codeChallenge = generateCodeChallenge(codeVerifier);
        } catch (NoSuchAlgorithmException e) {
            log.error("获取codeChallenge失败", e);
            throw new RuntimeException("获取codeChallenge失败");
        }

        // 获取授权码
        String authorizationCode = getAuthorizationCode(state, codeChallenge, headers);
        log.info("Authorization Code success: ");

        // 获取访问令牌
        JSONObject tokens = getTokens(authorizationCode, codeVerifier, headers);
        String accessToken = tokens.getStr("access_token");
        log.info("Access Token success: ");

        // 使用访问令牌上传文件
        return uploadFile(currentCode, accessToken, file, headers);

    }

    private static String generateState() {
        return Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String generateCodeVerifier() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static String getAuthorizationCode(String state, String codeChallenge, Map<String, String> headers) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("client_id", CLIENT_ID);
        requestBody.put("redirect_uri", REDIRECT_URI);
        requestBody.put("response_type", "code");
        requestBody.put("response_mode", "web_message");
        requestBody.put("code_challenge", codeChallenge);
        requestBody.put("state", state);
        requestBody.put("scope", SCOPE);

        HttpResponse response = getAuthorize(headers, requestBody, AUTHORIZATION_URL);
        if (response.getStatus() != 200) {
            log.error("获取Authorization Code失败: " + response.body());
            throw new RuntimeException("获取Authorization Code失败");
        }
        // 假设响应中包含授权码
        String body = response.body();
        JSONObject json = new JSONObject(body);
        return json.getStr("code");
    }

    private static HttpResponse getAuthorize(Map<String, String> headers, JSONObject requestBody, String authorizationUrl) {
        HttpResponse response = HttpRequest.post(authorizationUrl)
                .header("accept", "*/*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("cache-control", "no-cache")
                .header("content-type", "application/json")
                .header("cookie", headers.get("cookie"))
                .header("origin", "https://app.grammarly.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://app.grammarly.com/")
                .header("sec-ch-ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("x-client-type", "webeditor_chrome")
                .header("x-client-version", "1.5.43-6076+master")
                .header("x-container-id", headers.get("x-container-id"))
                .header("x-csrf-token", headers.get("x-csrf-token"))
                .body(requestBody.toString())
                .execute();
        return response;
    }

    private static JSONObject getTokens(String authorizationCode, String codeVerifier, Map<String, String> headers) {
        JSONObject requestBody = new JSONObject()
                .put("client_id", CLIENT_ID)
                .put("grant_type", "authorization_code")
                .put("code_verifier", codeVerifier)
                .put("authorization_code", authorizationCode);

        HttpResponse response = getAuthorize(headers, requestBody, TOKEN_URL);
        if (response.getStatus() != 200) {
            log.error("获取Access Token失败: " + response.body());
            throw new RuntimeException("获取Access Token失败");
        }
        return new JSONObject(response.body());
    }

    private static GrammarlyDocumentRes uploadFile(Code currentCode, String accessToken, MultipartFile file, Map<String, String> headers) {

        File convFile;
        try {
            convFile = CookieParse.convertToFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HttpResponse response = HttpRequest.post(UPLOAD_URL)
                .header("accept", "application/json")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("authorization", "Bearer " + accessToken)
                .header("cache-control", "no-cache")
                .header("content-type", "multipart/form-data")
                .header("cookie", headers.get("cookie"))
                .header("origin", "https://app.grammarly.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://app.grammarly.com/")
                .header("sec-ch-ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("x-api-version", headers.get("x-api-version"))
                .header("x-client-type", "denali_editor")
                .header("x-client-version", "1.5.43-6076+master")
                .header("x-container-id", headers.get("x-container-id"))
                .header("x-csrf-token", headers.get("x-csrf-token"))
                .form("source_file", convFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", file.getOriginalFilename())
                .execute();
        if (response.getStatus() != 201) {
            convFile.delete();
            log.error("上传Grammarly失败: {}", response.body());
            throw new RuntimeException("上传Grammarly失败,请重试");
        }
        JSONObject json = new JSONObject(response.body());
        convFile.delete();
        currentCode.setFileName(convFile.getName());
        return GrammarlyDocumentRes.builder()
                .document_id(json.getStr("id"))
                .title(json.getStr("title"))
                .size(json.getInt("size"))
                .is_deleted(json.getStr("is_deleted"))
                .build();
    }

    public static void main(String[] args) {
        Map<String, String> headers = new HashMap<>();
        headers.put("authority", "dox.grammarly.com");
        headers.put("accept", "application/json");
        headers.put("accept-language", "en");
        headers.put("authorization", "Bearer eyJ2ZXIiOiJWMyIsInR5cCI6ImF0K2p3dCIsInZlcnNpb24iOiIzIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJIeTRiMWdwZmx5a196aXlOTV9mR2xTUWhUamdwY0lDWkcxSGcwSWkwOUdvIn0.eyJzdWIiOiIxNDAyNTc1MjM2Iiwic2NwIjoiZ3JhbW1hcmx5LmNhcGkuYWxsIiwicnRpIjoiNjE5NzE5ZjctYjg5MC00NDMwLWI5MjItZWY2YTkwZjI0OTNkIiwib3JpZ2luIjoiaHR0cHM6Ly9hcHAuZ3JhbW1hcmx5LmNvbSIsImlzcyI6InRva2Vucy5ncmFtbWFybHkuY29tIiwic3NvX2lkIjoiZjU1NzhhZjUtZDZjNC00OTBmLTlkZmQtNmRhYjg0YmQ0ZWIzIiwiYXVkIjoiKi5ncmFtbWFybHkuY29tIiwicHJlbWl1bSI6dHJ1ZSwiZXhwIjoxNzIxMTE0NjQxLCJpYXQiOjE3MjExMTQzNDEsImp0aSI6ImI1NDFiNWM2LWZlNzEtNDM0Ni04M2Y3LWE3YjYzZGU1NmEyZCIsImNpZCI6IndlYmVkaXRvcl9jaHJvbWUifQ.c7YfXQ2EkvoRbijD3qOS_ioyn7S6WggQOPm0-cLKicYJedNahtBv4OUYhSICkyv0IlcgXSC_6oVSTzvUamjC5_l6lhD-0bn8_cFEFhoIxze7V66ucfL2fhdKoh2d-9A-mA3tmVsDLham1AI1O9aKaq8iUHNWDTdtEizDu9NznnIBr-uX19MC1dEe7QoUoe74XZt11scSzhjpqqZj-mFCebwMVvLmcpkKfQNMwk3dyRyCnw2WwP5LFCF8hbDhHWiXDCzPO0W745QuoA1lGq46623LTKpcnCjH_XAnhcDRY01Zk_kqPXZmLPLBVYROUDvLKzbAzRrIjosdas2osqFhjw");
        headers.put("origin", "https://app.grammarly.com");
        headers.put("referer", "https://app.grammarly.com/");
        headers.put("sec-ch-ua", "\"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"114\", \"Google Chrome\";v=\"114\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "same-site");
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        headers.put("Content-Type", "application/json");
        headers.put("cookie", "gnar_containerId=wdnn8q1vq3mj01g0; ga_clientId=137290076.1711344114; _fbp=fb.1.1711370913066.340165000; _pin_unauth=dWlkPU5tVXdPR1E1WkRJdE9HSm1aaTAwTUdNMkxXRmpObVF0TjJSaFpHRTRabU0yWXpSaQ; __q_state_SWP2vgTKwt4Sucin=eyJ1dWlkIjoiZTg4MjFjOGUtZTBiZC00ZTkyLTg4NDUtZDRkYmYxNzE1ZGI4IiwiY29va2llRG9tYWluIjoiZ3JhbW1hcmx5LmNvbSIsIm1lc3NlbmdlckV4cGFuZGVkIjpmYWxzZSwicHJvbXB0RGlzbWlzc2VkIjp0cnVlLCJjb252ZXJzYXRpb25JZCI6IjEzNjIyMTc2MjE2MzMzOTI5NjcifQ==; drift_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; driftt_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; gac=AABNaF5ZW5PeNvL3g1iZCjqsHkHKf6G3gWz7AQSLbuxfPpqwIBSKWiugmIt0jBTrygPf92kZcN8nfWJkficJIZyJ40MGlhRH77BtdH47O8DzuQdyjzmPQLMICokRavDLD_x39lx3hq-rPMhVPDTEPlVK69a3W6zujyk3D8ZHcDxNcvWHhfmfLfSj4fuRiGOJcjW59OUexvwhjcJaYJ4d1EL_pmgY9bBrMDd8MKLI06KKWAKJ-iA7oHryP7EwPurFQUeYbLqwYA2scS5XydG4YapYfOxjeWeCdTv5VXF0f3vIIRJ_; _ga_3X1EDE2ENQ=GS1.1.1713064447.2.1.1713064722.0.0.0; last_authn_event=ae086c2b-c9d8-4de0-83cb-e1fff2cd4e44; _gcl_au=1.1.537405054.1720443639; grauth=AABNyuC2D0Rig_9Qj58dKr1lb1rwF2EmcMXfDdOWjROcODjPYbM5TIkgHdgPOx92Ku7We12ibch4S_iY; csrf-token=AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA; funnelType=free; _gid=GA1.2.720341937.1721027893; _clck=1p32p0x%7C2%7Cfnh%7C0%7C1545; funnel_firstTouchUtmSource=funnel; tdi=hobhl2hmxzbvrnoxs; OptanonConsent=isGpcEnabled=0&datestamp=Mon+Jul+15+2024+15%3A20%3A51+GMT%2B0800+(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)&version=202405.2.0&browserGpcFlag=0&isIABGlobal=false&hosts=&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0003%3A1%2CC0004%3A1&AwaitingReconsent=false&geolocation=CN%3BZJ; OptanonAlertBoxClosed=2024-07-15T07:20:51.814Z; _tq_id.TV-7281365481-1.3988=1d2ab3b45e51429d.1711344112.0.1721028054..; _rdt_uuid=1711370912776.0db9a4e9-23a8-4e30-964f-1bf2fd7771a3; _uetsid=6c144fb0427a11efb7afd16695d7496b; _uetvid=960368c0ea6711eea7a109c79abde949; _clsk=ymlnyv%7C1721028060728%7C3%7C0%7Cv.clarity.ms%2Fcollect; _ga_CBK9K2ZWWE=GS1.1.1721027899.72.1.1721028149.60.0.0; _ga=GA1.2.137290076.1711344114; experiment_groups=fsrw_in_sidebar_allusers_enabled|extension_new_rich_text_fields_enabled|gdocs_for_chrome_enabled|officeaddin_outcomes_ui_exp5_enabled1|gb_tone_detector_onboarding_flow_enabled|completions_beta_enabled|premium_ungating_renewal_notification_enabled|kaza_security_hub_enabled|quarantine_messages_enabled|small_hover_menus_existing_enabled|gb_snippets_csv_upload_enabled|grammarly_web_ukraine_logo_dapi_enabled|officeaddin_upgrade_state_exp2_enabled1|gb_in_editor_premium_Test1|gb_analytics_mvp_phase_one_enabled|gb_rbac_new_members_ui_enabled|ipm_extension_release_test_1|snippets_in_ws_gate_enabled|extension_assistant_bundles_all_enabled|gb_analytics_group_filters_enabled|officeaddin_proofit_exp3_enabled|gdocs_for_all_firefox_enabled|gb_snippets_cycle_one_enabled|shared_workspaces_enabled|gb_analytics_mvp_phase_one_30_day_enabled|auto_complete_correct_safari_enabled|fluid_gdocs_rollout_enabled|officeaddin_ue_exp3_enabled|safari_migration_inline_disabled_enabled|officeaddin_upgrade_state_exp1_enabled1|completions_release_enabled1|fsrw_in_assistant_all_enabled|autocorrect_new_ui_v3|emogenie_beta_enabled|apply_formatting_all_enabled|shadow_dom_chrome_enabled|extension_assistant_experiment_all_enabled|gdocs_for_all_safari_enabled|extension_assistant_all_enabled|safari_migration_backup_notif1_enabled|auto_complete_correct_edge_enabled|takeaways_premium_enabled|realtime_proofit_external_rollout_enabled|safari_migration_popup_editor_disabled_enabled|safari_migration_inline_warning_enabled|llama_beta_managed_test_1|gdocs_new_mapping_enabled|officeaddin_muted_alerts_exp2_enabled1|officeaddin_perf_exp3_enabled|gb_expanded_analytics_enabled; redirect_location=eyJ0eXBlIjoiIiwibG9jYXRpb24iOiJodHRwczovL2FwcC5ncmFtbWFybHkuY29tLyJ9; browser_info=CHROME:126:COMPUTER:SUPPORTED:FREEMIUM:MAC_OS_X:MAC_OS_X; cookie=");
        headers.put("Accept", "text/plain,application/octet-stream,application/vnd,application/pdf,application/json,application/zip")
        ;
        headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Length", "315284");
        headers.put("Content-Type", "application/json");

        headers.put("x-csrf-token", "AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA");
        headers.put("X-Client-Type", "denali_editor");
        headers.put("X-Api-Version", "2");
        headers.put("X-Client-Version", "1.5.43-6077+master");
        headers.put("X-Container-Id", "wdnn8q1vq3mj01g0");
        headers.put("Origin", "https://app.grammarly.com");
        headers.put("Pragma", "no-cache");
        headers.put("Priority", "u=1, i");
        headers.put("Referer", "https://app.grammarly.com/");
        headers.put("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");


        String jsonData = "{\"html\":\"<your-html-content>\"}";

        String url = "https://dox.grammarly.com/documents/2528194383/report";

        HttpResponse response = HttpRequest.post(url)
                .header("accept", "text/plain,application/octet-stream,application/vnd,application/pdf,application/json,application/zip")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("authorization", "Bearer eyJ2ZXIiOiJWMyIsInR5cCI6ImF0K2p3dCIsInZlcnNpb24iOiIzIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJIeTRiMWdwZmx5a196aXlOTV9mR2xTUWhUamdwY0lDWkcxSGcwSWkwOUdvIn0.eyJzdWIiOiIxNDAyNTc1MjM2Iiwic2NwIjoiZ3JhbW1hcmx5LmNhcGkuYWxsIiwicnRpIjoiMWRhM2IyMGUtOGQ3Mi00NTljLWFmMGMtZTA2Mzg3YTJkOGMzIiwib3JpZ2luIjoiaHR0cHM6Ly9hcHAuZ3JhbW1hcmx5LmNvbSIsImlzcyI6InRva2Vucy5ncmFtbWFybHkuY29tIiwic3NvX2lkIjoiZjU1NzhhZjUtZDZjNC00OTBmLTlkZmQtNmRhYjg0YmQ0ZWIzIiwiYXVkIjoiKi5ncmFtbWFybHkuY29tIiwicHJlbWl1bSI6dHJ1ZSwiZXhwIjoxNzIxMTE4MDY1LCJpYXQiOjE3MjExMTc3NjUsImp0aSI6ImUwNzVhMWJjLTBkMjctNGU0OS04Y2M1LTI1ODg0MzlkNmZmYSIsImNpZCI6IndlYmVkaXRvcl9jaHJvbWUifQ.pN_pFwP0bQYg76KNIMOURdm1bbaYZoPI2M3jShIZMsdOapAKQgx0v36WEfwkoxv7nBhp2DdBd798YiTljqA6ksfw7gFkknRosK6-ixUQvRr6ZaMkWM7EcILHKsKROWsUXWM2u6sm1aFJ1DRTOSRC2dIVaMBfFvyyIoNa89cQDhpr54dC_h3iHZ3CO8kOW8kTDkRQNL8vF4c64cwqf5pXw91RWHMo3jIm-I8pOYwohNyQSpNEIPbeghfAaZRcFbyLKaGxs6j2ct3JyVUvjLWxZaH_CZBGhCYMSMCPzHpizVL9Z_1nUkKpyKaHrm6umYLv7WGgjsXu-Iqd5FOrbP8cgw")
                .header("cache-control", "no-cache")
                .header("content-type", "application/json")
                .header("cookie", "gnar_containerId=wdnn8q1vq3mj01g0; ga_clientId=137290076.1711344114; _fbp=fb.1.1711370913066.340165000; _pin_unauth=dWlkPU5tVXdPR1E1WkRJdE9HSm1aaTAwTUdNMkxXRmpObVF0TjJSaFpHRTRabU0yWXpSaQ; __q_state_SWP2vgTKwt4Sucin=eyJ1dWlkIjoiZTg4MjFjOGUtZTBiZC00ZTkyLTg4NDUtZDRkYmYxNzE1ZGI4IiwiY29va2llRG9tYWluIjoiZ3JhbW1hcmx5LmNvbSIsIm1lc3NlbmdlckV4cGFuZGVkIjpmYWxzZSwicHJvbXB0RGlzbWlzc2VkIjp0cnVlLCJjb252ZXJzYXRpb25JZCI6IjEzNjIyMTc2MjE2MzMzOTI5NjcifQ==; drift_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; driftt_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; gac=AABNaF5ZW5PeNvL3g1iZCjqsHkHKf6G3gWz7AQSLbuxfPpqwIBSKWiugmIt0jBTrygPf92kZcN8nfWJkficJIZyJ40MGlhRH77BtdH47O8DzuQdyjzmPQLMICokRavDLD_x39lx3hq-rPMhVPDTEPlVK69a3W6zujyk3D8ZHcDxNcvWHhfmfLfSj4fuRiGOJcjW59OUexvwhjcJaYJ4d1EL_pmgY9bBrMDd8MKLI06KKWAKJ-iA7oHryP7EwPurFQUeYbLqwYA2scS5XydG4YapYfOxjeWeCdTv5VXF0f3vIIRJ_; _ga_3X1EDE2ENQ=GS1.1.1713064447.2.1.1713064722.0.0.0; last_authn_event=ae086c2b-c9d8-4de0-83cb-e1fff2cd4e44; _gcl_au=1.1.537405054.1720443639; grauth=AABNyuC2D0Rig_9Qj58dKr1lb1rwF2EmcMXfDdOWjROcODjPYbM5TIkgHdgPOx92Ku7We12ibch4S_iY; csrf-token=AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA; funnelType=free; _gid=GA1.2.720341937.1721027893; _clck=1p32p0x%7C2%7Cfnh%7C0%7C1545; tdi=hobhl2hmxzbvrnoxs; OptanonConsent=isGpcEnabled=0&datestamp=Mon+Jul+15+2024+15%3A20%3A51+GMT%2B0800+(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)&version=202405.2.0&browserGpcFlag=0&isIABGlobal=false&hosts=&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0003%3A1%2CC0004%3A1&AwaitingReconsent=false&geolocation=CN%3BZJ; OptanonAlertBoxClosed=2024-07-15T07:20:51.814Z; _tq_id.TV-7281365481-1.3988=1d2ab3b45e51429d.1711344112.0.1721028054..; _rdt_uuid=1711370912776.0db9a4e9-23a8-4e30-964f-1bf2fd7771a3; _uetvid=960368c0ea6711eea7a109c79abde949; _ga_CBK9K2ZWWE=GS1.1.1721027899.72.1.1721028149.60.0.0; _ga=GA1.2.137290076.1711344114; redirect_location=eyJ0eXBlIjoiIiwibG9jYXRpb24iOiJodHRwczovL2FwcC5ncmFtbWFybHkuY29tL2Rkb2NzLzI1MjgxOTQzODMifQ==; browser_info=CHROME:126:COMPUTER:SUPPORTED:FREEMIUM:MAC_OS_X:MAC_OS_X; experiment_groups=fsrw_in_sidebar_allusers_enabled|extension_new_rich_text_fields_enabled|gdocs_for_chrome_enabled|officeaddin_outcomes_ui_exp5_enabled1|gb_tone_detector_onboarding_flow_enabled|completions_beta_enabled|premium_ungating_renewal_notification_enabled|kaza_security_hub_enabled|quarantine_messages_enabled|small_hover_menus_existing_enabled|gb_snippets_csv_upload_enabled|grammarly_web_ukraine_logo_dapi_enabled|officeaddin_upgrade_state_exp2_enabled1|gb_in_editor_premium_Test1|gb_analytics_mvp_phase_one_enabled|gb_rbac_new_members_ui_enabled|ipm_extension_release_test_1|snippets_in_ws_gate_enabled|extension_assistant_bundles_all_enabled|gb_analytics_group_filters_enabled|officeaddin_proofit_exp3_enabled|gdocs_for_all_firefox_enabled|gb_snippets_cycle_one_enabled|shared_workspaces_enabled|gb_analytics_mvp_phase_one_30_day_enabled|auto_complete_correct_safari_enabled|fluid_gdocs_rollout_enabled|officeaddin_ue_exp3_enabled|disable_extension_installation_disabled|safari_migration_inline_disabled_enabled|officeaddin_upgrade_state_exp1_enabled1|completions_release_enabled1|fsrw_in_assistant_all_enabled|autocorrect_new_ui_v3|emogenie_beta_enabled|apply_formatting_all_enabled|shadow_dom_chrome_enabled|extension_assistant_experiment_all_enabled|gdocs_for_all_safari_enabled|extension_assistant_all_enabled|safari_migration_backup_notif1_enabled|auto_complete_correct_edge_enabled|takeaways_premium_enabled|realtime_proofit_external_rollout_enabled|safari_migration_popup_editor_disabled_enabled|safari_migration_inline_warning_enabled|llama_beta_managed_test_1|gdocs_new_mapping_enabled|officeaddin_muted_alerts_exp2_enabled1|officeaddin_perf_exp3_enabled|gb_expanded_analytics_enabled; cookie=")
                .header("origin", "https://app.grammarly.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://app.grammarly.com/")
                .header("sec-ch-ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("x-api-version", "2")
                .header("x-client-type", "denali_editor")
                .header("x-client-version", "1.5.43-6077+master")
                .header("x-container-id", "wdnn8q1vq3mj01g0")
                .header("x-csrf-token", "AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA")
                .execute();

        String body = response.body();
        System.out.println(response);

    }
}
