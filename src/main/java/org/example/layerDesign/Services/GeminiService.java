package org.example.layerDesign.Services;

import okhttp3.*;
import org.example.layerDesign.Helper.ConfigReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class GeminiService {
    private static final String GEMINI_API_KEY = ConfigReader.get("GEMINI_API_KEY");
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    public String askGemini(String promptText) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Gemini API için istek gövdesi oluşturma
        JSONObject part = new JSONObject();
        part.put("text", promptText);

        JSONArray partsArray = new JSONArray();
        partsArray.put(part);

        JSONObject content = new JSONObject();
        content.put("parts", partsArray);
        // Gemini API'nin bazı modelleri için 'role' belirtmek gerekebilir.
        // content.put("role", "user");

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(content);

        JSONObject requestBodyJson = new JSONObject();
        requestBodyJson.put("contents", contentsArray);

        // İsteğe bağlı: Üretilecek metnin özelliklerini ayarlamak için
        // JSONObject generationConfig = new JSONObject();
        // generationConfig.put("maxOutputTokens", 100);
        // generationConfig.put("temperature", 0.7); // 0.0 ile 1.0 arası
        // requestBodyJson.put("generationConfig", generationConfig);

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBodyJson.toString(), mediaType);

        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                // API anahtarı URL'de olduğu için ayrıca bir Authorization header'ına gerek yok
                // Eğer API anahtarını header ile göndermek isterseniz:
                // .addHeader("x-goog-api-key", GEMINI_API_KEY) // Bu durumda URL'den ?key= kısmını çıkarın
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) { // OkHttp Response'u
            if (!response.isSuccessful()) {
                System.err.println("Gemini API isteği başarısız oldu: " + response.code());
                System.err.println("Yanıt: " + response.body().string());
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            System.out.println("Gemini'nin ham yanıtı:");
            System.out.println(responseBody);

            // Gemini yanıtını ayrıştırma
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (jsonResponse.has("candidates") && jsonResponse.getJSONArray("candidates").length() > 0) {
                JSONObject firstCandidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
                if (firstCandidate.has("content") && firstCandidate.getJSONObject("content").has("parts") &&
                        firstCandidate.getJSONObject("content").getJSONArray("parts").length() > 0) {
                    String geminiResponseText = firstCandidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
                    System.out.println("\nGemini'nin cevabı:");
                    System.out.println(geminiResponseText);
                } else {
                    System.out.println("\nGemini'nin yanıtında beklenen 'parts' alanı bulunamadı.");
                }
            } else if (jsonResponse.has("promptFeedback")) {
                System.out.println("\nGemini'den geçerli bir aday yanıt alınamadı. Geri bildirim:");
                System.out.println(jsonResponse.getJSONObject("promptFeedback").toString(2));
            }
            else {
                System.out.println("\nGemini'nin yanıt formatı beklenenden farklı.");
            }
        }
        return promptText;
    }
}
