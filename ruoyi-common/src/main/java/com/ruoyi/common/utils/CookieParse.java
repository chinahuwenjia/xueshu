package com.ruoyi.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CookieParse {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int RANDOM_FILE_NAME_LENGTH = 10;

    public static Map<String, String> convertCurlToMap(String curl) {
        Map<String, String> headers = new HashMap<>();

        // Split the curl headers string by the "-H" delimiter
        String[] lines = curl.split("-H '");
        // Process each line to extract the header key and value
        for (String line : lines) {
            // Skip empty or invalid lines
            if (line.trim().isEmpty() || !line.contains(": ")) {
                continue;
            }

            // Split the line into key and value
            int colonIndex = line.indexOf(": ");
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 2).replaceAll("'\\\\?\\s*$", "").trim();

            // Add the key-value pair to the headers map
            headers.put(key, value);
        }

        return headers;
    }

    public static Map<String, String> parseCookies(String cookieString) {
        Map<String, String> cookieMap = new HashMap<>();
        String[] cookies = cookieString.split("; ");
        for (String cookie : cookies) {
            String[] keyValue = cookie.split("=", 2);
            if (keyValue.length == 2) {
                cookieMap.put(keyValue[0], keyValue[1]);
            } else {
                cookieMap.put(keyValue[0], "");
            }
        }
        return cookieMap;
    }


    public static File convertToFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        // Remove all dots from the original file name
        String sanitizedFileName = originalFileName != null ? originalFileName.replaceAll("\\.", "") : "";
        String prefix = (sanitizedFileName.length() >= 2) ? sanitizedFileName.substring(0, 2) : "";
        String randomFileName = generateRandomFileName(RANDOM_FILE_NAME_LENGTH);
        String newFileName = prefix + randomFileName;
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + newFileName);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        log.info("Converted file to: {}", convFile.getAbsolutePath());
        log.info("File size: {}", convFile.getName());
        return convFile;
    }

    private static String generateRandomFileName(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        String encodedString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        // Ensure the encoded string is exactly the desired length
        return encodedString.substring(0, length);
    }
}
