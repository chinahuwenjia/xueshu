package com.ruoyi.system.service.token;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class FileDownloader {

    private static final int TIMEOUT = 10000; // 10 seconds timeout

    public static void main(String[] args) {
        String fileURL = "https://dox.grammarly.com/documents/2528194383/report";
        String saveDir = "/Users/huwenjia/Downloads/";
        String fileName = "AU_2012308434_B2";

        try {
            downloadFileWithTimeout(fileURL, saveDir, fileName, TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFileWithTimeout(String fileURL, String saveDir, String fileName, int timeout) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Future<?> future = executor.submit(() -> {
            try {
                downloadFile(fileURL, saveDir, fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.cancel(true);
                System.out.println("Download timed out");
            }
        }, timeout, TimeUnit.MILLISECONDS);

        try {
            future.get(); // Block until the future is done or timeout occurs
        } catch (CancellationException | InterruptedException | ExecutionException e) {
            System.out.println("Download task cancelled or interrupted");
        } finally {
            executor.shutdown();
            scheduler.shutdown();
        }
    }

    public static void downloadFile(String fileURL, String saveDir, String fileName) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        // Set headers
        httpConn.setRequestProperty("accept", "text/plain,application/octet-stream,application/vnd,application/pdf,application/json,application/zip");
        httpConn.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
        httpConn.setRequestProperty("authorization", "Bearer eyJ2ZXIiOiJWMyIsInR5cCI6ImF0K2p3dCIsInZlcnNpb24iOiIzIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJMS2RxT2xIU2U3TUtGa3FNQnVZeHJOWkpLOWhCS3pTTFlia1cyNXFVMlhZIn0.eyJzdWIiOiIxNDAyNTc1MjM2Iiwic2NwIjoiZ3JhbW1hcmx5LmNhcGkuYWxsIiwicnRpIjoiMWRhM2IyMGUtOGQ3Mi00NTljLWFmMGMtZTA2Mzg3YTJkOGMzIiwib3JpZ2luIjoiaHR0cHM6Ly9hcHAuZ3JhbW1hcmx5LmNvbSIsImlzcyI6InRva2Vucy5ncmFtbWFybHkuY29tIiwic3NvX2lkIjoiZjU1NzhhZjUtZDZjNC00OTBmLTlkZmQtNmRhYjg0YmQ0ZWIzIiwiYXVkIjoiKi5ncmFtbWFybHkuY29tIiwicHJlbWl1bSI6dHJ1ZSwiZXhwIjoxNzIxMTE5MzAzLCJpYXQiOjE3MjExMTkwMDMsImp0aSI6ImY4ZWE4MzM0LThjMTAtNGQwYS04MDA4LTE3ZWIwMjFmMTU0OCIsImNpZCI6IndlYmVkaXRvcl9jaHJvbWUifQ.1KxMsV8S0_DLHmFnsOZZsEaOr4vVR0_ORDeWzKb5q13iuAXj2yhyjPK8JOo8AlsJDIbXdzTIA8o01rPi0N6DBuiG_U8dq-jCLahifJFtclf4zVAfLZ6U8DLrgjnl_Z6V8nGa9OqBjAH_YbXgbj0M14MFcJozaRVy6kV6SzqUkm7fIu3LU3jYyUEkcnQ8fFbdnzOcPoVwqKDfTN85hR1i4CLqWNHL156vf6VKmgwkvx5h6w_ZgzzjkLmsyIv1hb0x-Rxj_F8HBhezOhb6cFY8fbRXZk2MQBD1K3Kh7TyA6iLmXbn5_E-t2lCivFmk2f9Nwq1GbzjE1UyYl4zQRptvHQ");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("content-type", "application/json");
        httpConn.setRequestProperty("cookie", "gnar_containerId=wdnn8q1vq3mj01g0; ga_clientId=137290076.1711344114; _fbp=fb.1.1711370913066.340165000; _pin_unauth=dWlkPU5tVXdPR1E1WkRJdE9HSm1aaTAwTUdNMkxXRmpObVF0TjJSaFpHRTRabU0yWXpSaQ; __q_state_SWP2vgTKwt4Sucin=eyJ1dWlkIjoiZTg4MjFjOGUtZTBiZC00ZTkyLTg4NDUtZDRkYmYxNzE1ZGI4IiwiY29va2llRG9tYWluIjoiZ3JhbW1hcmx5LmNvbSIsIm1lc3NlbmdlckV4cGFuZGVkIjpmYWxzZSwicHJvbXB0RGlzbWlzc2VkIjp0cnVlLCJjb252ZXJzYXRpb25JZCI6IjEzNjIyMTc2MjE2MzMzOTI5NjcifQ==; drift_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; driftt_aid=97bb7cc3-6187-4349-b9b5-d642e71eaf97; gac=AABNaF5ZW5PeNvL3g1iZCjqsHkHKf6G3gWz7AQSLbuxfPpqwIBSKWiugmIt0jBTrygPf92kZcN8nfWJkficJIZyJ40MGlhRH77BtdH47O8DzuQdyjzmPQLMICokRavDLD_x39lx3hq-rPMhVPDTEPlVK69a3W6zujyk3D8ZHcDxNcvWHhfmfLfSj4fuRiGOJcjW59OUexvwhjcJaYJ4d1EL_pmgY9bBrMDd8MKLI06KKWAKJ-iA7oHryP7EwPurFQUeYbLqwYA2scS5XydG4YapYfOxjeWeCdTv5VXF0f3vIIRJ_; _ga_3X1EDE2ENQ=GS1.1.1713064447.2.1.1713064722.0.0.0; last_authn_event=ae086c2b-c9d8-4de0-83cb-e1fff2cd4e44; _gcl_au=1.1.537405054.1720443639; grauth=AABNyuC2D0Rig_9Qj58dKr1lb1rwF2EmcMXfDdOWjROcODjPYbM5TIkgHdgPOx92Ku7We12ibch4S_iY; csrf-token=AABNyppxtLJlWqQdcXSsa5ehM4L7E8zULRF5MA; funnelType=free; _gid=GA1.2.720341937.1721027893; _clck=1p32p0x%7C2%7Cfnh%7C0%7C1545; tdi=hobhl2hmxzbvrnoxs; OptanonConsent=isGpcEnabled=0&datestamp=Mon+Jul+15+2024+15%3A20%3A51+GMT%2B0800+(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)&version=202405.2.0&browserGpcFlag=0&isIABGlobal=false&hosts=&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0003%3A1%2CC0004%3A1&AwaitingReconsent=false");
        httpConn.setRequestProperty("pragma", "no-cache");
        httpConn.setRequestProperty("referer", "https://app.grammarly.com/");
        httpConn.setRequestProperty("sec-ch-ua", "\"Google Chrome\";v=\"114\", \"Chromium\";v=\"114\", \"Not:A-Brand\";v=\"99\"");
        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
        httpConn.setRequestProperty("sec-fetch-dest", "empty");
        httpConn.setRequestProperty("sec-fetch-mode", "cors");
        httpConn.setRequestProperty("sec-fetch-site", "same-site");
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");

        httpConn.setDoOutput(true);

        String requestBody = "{\"searchContext\":{\"content\":null,\"path\":null},\"selection\":null,\"applyFormatDirectly\":false,\"applyChangesFromChecked\":false}";
        try (OutputStream os = httpConn.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }


        int responseCode = httpConn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = httpConn.getInputStream()) {
                String saveFilePath = saveDir + fileName;

                try (FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {
                    int bytesRead;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("File downloaded to " + saveFilePath);
            }
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }

        httpConn.disconnect();
    }
}

