package org.example.layerDesign.Helper;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigReader {

    private static JSONObject config;

    static {
        try {
            String content = new String(Files.readAllBytes(Paths.get("/Users/onursafran/Desktop/aiAgentProject/src/main/resources/config.json")));
            config = new JSONObject(content);
        } catch (IOException e) {
            System.err.println("config.json okunamadı: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return config.optString(key, null);
    }
}
