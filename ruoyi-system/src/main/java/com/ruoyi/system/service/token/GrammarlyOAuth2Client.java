package com.ruoyi.system.service.token;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.CookieParse;
import com.ruoyi.system.domain.grammarly.AuthorizeDTO;
import com.ruoyi.system.domain.grammarly.TokenDTO;
import com.ruoyi.system.domain.grammarly.UserDTO;
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
        String codeVerifier = generateCodeVerifier();
        // 获取授权码
        String authorizationCode = getAuthorizationCode(codeVerifier, headers).getCode();
        log.info("Authorization Code success: ");
        // 获取访问令牌
        String accessToken = getTokens(authorizationCode, codeVerifier, headers).getAccess_token();
        log.info("Access Token success: ");
        // 使用访问令牌上传文件
        return uploadFile(currentCode, accessToken, file, headers);

    }

    private static String generateState() {
        return Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String generateCodeVerifier() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    public static AuthorizeDTO getAuthorizationCode(String codeVerifier, Map<String, String> headers) {
        String state = generateState();
        String codeChallenge;
        try {
            codeChallenge = generateCodeChallenge(codeVerifier);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

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
            log.error("获取Authorization Code失败: {}", response.body());
            throw new RuntimeException("获取Authorization Code失败");
        }
        // 假设响应中包含授权码
        String body = response.body();
        JSONObject json = new JSONObject(body);
        AuthorizeDTO authorizeDTO = new AuthorizeDTO();
        authorizeDTO.setCode(json.getStr("code"));
        authorizeDTO.setState(json.getStr("state"));
        return authorizeDTO;
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

    public static TokenDTO getTokens(String authorizationCode, String codeVerifier, Map<String, String> headers) {
        JSONObject requestBody = new JSONObject()
                .put("client_id", CLIENT_ID)
                .put("grant_type", "authorization_code")
                .put("code_verifier", codeVerifier)
                .put("authorization_code", authorizationCode);
        HttpResponse response = getAuthorize(headers, requestBody, TOKEN_URL);
        if (response.getStatus() != 200) {
            log.error("获取Access Token失败: {}", response.body());
            throw new RuntimeException("获取Access Token失败");
        }
        return JSONUtil.parseObj(response.body()).toBean(TokenDTO.class);
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


    public static UserDTO getUser(Map<String, String> headers) {
        String url = "https://auth.grammarly.com/v3/user?app=webeditor_chrome&field=sandbox.payments&field=frontend_role";
        // 构建请求
        HttpRequest request = HttpUtil.createGet(url)
                .header("accept", "application/json")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("cache-control", "no-cache")
                .header("origin", "https://app.grammarly.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://app.grammarly.com/")
                .header("sec-ch-ua", headers.get("sec-ch-ua"))
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("cookie", headers.get("cookie"))
                .header("sec-fetch-site", "same-site")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("x-client-type", "webeditor_chrome")
                .header("x-client-version", "1.5.43-6111+master")
                .header("x-container-id", headers.get("x-container-id"))
                .header("x-csrf-token", headers.get("x-csrf-token"));

        // 发送请求并接收响应
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("获取用户信息失败: {}", response.body());
            throw new RuntimeException("获取用户信息失败");
        }
        log.info("获取用户信息成功: ");
        return JSONUtil.parseObj(response.body()).toBean(UserDTO.class);
    }


    public static void main(String[] args) {
        String curl = "curl 'https://subscription.grammarly.com/api/v1/subscription' \\\n" +
                "  -H 'accept: application/json' \\\n" +
                "  -H 'accept-language: zh-CN,zh;q=0.9' \\\n" +
                "  -H 'authorization: Bearer eyJ2ZXIiOiJWMyIsInR5cCI6ImF0K2p3dCIsInZlcnNpb24iOiIzIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJEODNYQUlOZGdHWlRxME5DRm9DT0tTbFNSeUFNSWQ0a0E1MEZ0NnFwOGdFIn0.eyJzdWIiOiIxNDAyNTc1MjM2Iiwic2NwIjoiZ3JhbW1hcmx5LmNhcGkuYWxsIiwicnRpIjoiYzQwM2VkZDktZjEwOS00ZTE5LTlhZDMtYTg0YjUwZTlmNjg0Iiwib3JpZ2luIjoiaHR0cHM6Ly9hcHAuZ3JhbW1hcmx5LmNvbSIsImlzcyI6InRva2Vucy5ncmFtbWFybHkuY29tIiwic3NvX2lkIjoiZjU1NzhhZjUtZDZjNC00OTBmLTlkZmQtNmRhYjg0YmQ0ZWIzIiwiYXVkIjoiKi5ncmFtbWFybHkuY29tIiwicHJlbWl1bSI6dHJ1ZSwiZXhwIjoxNzIxNzE5MjI3LCJpYXQiOjE3MjE3MTg5MjcsImp0aSI6IjhmOWQ4NWFhLThiZmQtNGE2ZC05OGJjLTRlMTYyNjA5YTc0YyIsImNpZCI6IndlYmVkaXRvcl9jaHJvbWUifQ.j6qLyT--2svrrO2xpYWU1bpe2U7it_W5shu1dc2_EJNOvzkS55MH_ZiVFErFpUrbkxh-rBGu7r-zBzPRBADKwc-ViNeJflSSWXFO4rogWBoOtgrKpaT0kP80WB0dHsn9_O01u-2dSL2Z8wVIvPKVp1FQ6usi6BY15QYAQDtiqTIYrJKBgrFEK__bVHdeTqi2molCSpNC9JbCd6XyWHzmqHKYK3PSIAr-pG_63UXWz-bohimkoPm7-ZxrZwY2muogUQIfcel3Lm5yv1Z-FhGh_KLMlCrgbIzx39QiaN2oXrCYkhgqs_RymVIJTcKDlO8BrhOpDqgiq9Ujaf9pL10BiQ' \\\n" +
                "  -H 'cache-control: no-cache' \\\n" +
                "  -H 'cookie: gnar_containerId=wdnn8q1vq3mj01g0; ga_clientId=137290076.1711344114; _fbp=fb.1.1711370913066.340165000; _pin_unauth=dWlkPU5tVXdPR1E1WkRJdE9HSm1aaTAwTUdNMkxXRmpObVF0TjJSaFpHRTRabU0yWXpSaQ; __q_state_SWP2vgTKwt4Sucin=eyJ1dWlkIjoiZTg4MjFjOGUtZTBiZC00ZTkyLTg4NDUtZDRkYmYxNzE1ZGI4IiwiY29va2llRG9tYWluIjoiZ3JhbW1hcmx5LmNvbSIsIm1lc3NlbmdlckV4cGFuZGVkIjpmYWxzZSwicHJvbXB0RGlzbWlzc2VkIjp0cnVlLCJjb252ZXJzYXRpb25JZCI6IjEzNjIyMTc2MjE2MzMzOTI5NjcifQ==; drift_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; driftt_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; gac=AABNaF5ZW5PeNvL3g1iZCjqsHkHKf6G3gWz7AQSLbuxfPpqwIBSKWiugmIt0jBTrygPf92kZcN8nfWJkficJIZyJ40MGlhRH77BtdH47O8DzuQdyjzmPQLMICokRavDLD_x39lx3hq-rPMhVPDTEPlVK69a3W6zujyk3D8ZHcDxNcvWHhfmfLfSj4fuRiGOJcjW59OUexvwhjcJaYJ4d1EL_pmgY9bBrMDd8MKLI06KKWAKJ-iA7oHryP7EwPurFQUeYbLqwYA2scS5XydG4YapYfOxjeWeCdTv5VXF0f3vIIRJ_; last_authn_event=ae086c2b-c9d8-4de0-83cb-e1fff2cd4e44; _gcl_au=1.1.537405054.1720443639; grauth=AABNyuC2D0Rig_9Qj58dKr1lb1rwF2EmcMXfDdOWjROcODjPYbM5TIkgHdgPOx92Ku7We12ibch4S_iY; csrf-token=AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA; _clck=1p32p0x%7C2%7Cfnk%7C0%7C1545; tdi=pithr2hpm0vki1csg; _uetvid=960368c0ea6711eea7a109c79abde949; _ga_3X1EDE2ENQ=GS1.1.1721286115.3.1.1721286446.0.0.0; funnelType=free; _gid=GA1.2.1066465741.1721657200; OptanonAlertBoxClosed=2024-07-22T14:06:41.857Z; _rdt_uuid=1711370912776.0db9a4e9-23a8-4e30-964f-1bf2fd7771a3; OptanonConsent=isGpcEnabled=0&datestamp=Mon+Jul+22+2024+22%3A06%3A42+GMT%2B0800+(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)&version=202405.2.0&browserGpcFlag=0&isIABGlobal=false&hosts=&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0003%3A1%2CC0004%3A1&AwaitingReconsent=false&geolocation=CN%3BZJ; _ga=GA1.1.137290076.1711344114; _tq_id.TV-7281365481-1.3988=1d2ab3b45e51429d.1711344112.0.1721657205..; _ga_CBK9K2ZWWE=GS1.1.1721657202.76.0.1721657526.60.0.0; experiment_groups=fsrw_in_sidebar_allusers_enabled|extension_new_rich_text_fields_enabled|gdocs_for_chrome_enabled|officeaddin_outcomes_ui_exp5_enabled1|gb_tone_detector_onboarding_flow_enabled|completions_beta_enabled|premium_ungating_renewal_notification_enabled|kaza_security_hub_enabled|quarantine_messages_enabled|small_hover_menus_existing_enabled|gb_snippets_csv_upload_enabled|grammarly_web_ukraine_logo_dapi_enabled|officeaddin_upgrade_state_exp2_enabled1|gb_in_editor_premium_Test1|gb_analytics_mvp_phase_one_enabled|gb_rbac_new_members_ui_enabled|ipm_extension_release_test_1|snippets_in_ws_gate_enabled|extension_assistant_bundles_all_enabled|gb_analytics_group_filters_enabled|officeaddin_proofit_exp3_enabled|gdocs_for_all_firefox_enabled|gb_snippets_cycle_one_enabled|shared_workspaces_enabled|gb_analytics_mvp_phase_one_30_day_enabled|auto_complete_correct_safari_enabled|fluid_gdocs_rollout_enabled|officeaddin_ue_exp3_enabled|safari_migration_inline_disabled_enabled|officeaddin_upgrade_state_exp1_enabled1|completions_release_enabled1|fsrw_in_assistant_all_enabled|autocorrect_new_ui_v3|emogenie_beta_enabled|apply_formatting_all_enabled|shadow_dom_chrome_enabled|extension_assistant_experiment_all_enabled|gdocs_for_all_safari_enabled|extension_assistant_all_enabled|safari_migration_backup_notif1_enabled|auto_complete_correct_edge_enabled|takeaways_premium_enabled|realtime_proofit_external_rollout_enabled|safari_migration_popup_editor_disabled_enabled|safari_migration_inline_warning_enabled|llama_beta_managed_test_1|gdocs_new_mapping_enabled|officeaddin_muted_alerts_exp2_enabled1|officeaddin_perf_exp3_enabled|gb_expanded_analytics_enabled; redirect_location=eyJ0eXBlIjoiIiwibG9jYXRpb24iOiJodHRwczovL2FwcC5ncmFtbWFybHkuY29tLyJ9; browser_info=CHROME:126:COMPUTER:SUPPORTED:FREEMIUM:MAC_OS_X:MAC_OS_X; cookie=' \\\n" +
                "  -H 'origin: https://app.grammarly.com' \\\n" +
                "  -H 'pragma: no-cache' \\\n" +
                "  -H 'priority: u=1, i' \\\n" +
                "  -H 'referer: https://app.grammarly.com/' \\\n" +
                "  -H 'sec-ch-ua: \"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"' \\\n" +
                "  -H 'sec-ch-ua-mobile: ?0' \\\n" +
                "  -H 'sec-ch-ua-platform: \"macOS\"' \\\n" +
                "  -H 'sec-fetch-dest: empty' \\\n" +
                "  -H 'sec-fetch-mode: cors' \\\n" +
                "  -H 'sec-fetch-site: same-site' \\\n" +
                "  -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36' \\\n" +
                "  -H 'x-client-type: webeditor_chrome' \\\n" +
                "  -H 'x-client-version: 1.5.43-6111+master' \\\n" +
                "  -H 'x-container-id: wdnn8q1vq3mj01g0' \\\n" +
                "  -H 'x-csrf-token: AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA'";
        Map<String, String> headers = CookieParse.convertCurlToMap(curl);
        String codeVerifier = GrammarlyOAuth2Client.generateCodeVerifier();
        AuthorizeDTO authorizeDTO = GrammarlyOAuth2Client.getAuthorizationCode(codeVerifier, headers);
        System.out.println(authorizeDTO.toString());
        TokenDTO tokenDTO = GrammarlyOAuth2Client.getTokens(authorizeDTO.getCode(), codeVerifier, headers);
        System.out.println(tokenDTO.toString());
        System.out.println(getUser(headers));
    }

}
