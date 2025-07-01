package org.example.SingleClassStudy;

import io.restassured.RestAssured;
import okhttp3.*; // OkHttp'den gelen Request, Response, OkHttpClient, MediaType, RequestBody
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class WeatherAgent {

    // MGM endpoint
    private static final String WEATHER_URL = "https://servis.mgm.gov.tr/web/sondurumlar?merkezid=93401";
    // Gemini API Key - KENDİ GEMINI API ANAHTARINIZI BURAYA GİRİN!
    private static final String GEMINI_API_KEY = "AIzaSyB_Hbrpxby6sVvIhVwGd5CyGdBf8_LhHxs";
    // Gemini API Endpoint (gemini-pro modelini kullanıyoruz)
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    public static void main(String[] args) {
        String temperature = getTemperatureFromMGM();
        if (temperature != null) {
            try {
                sendPromptToGemini(temperature);
            } catch (IOException e) {
                System.err.println("Gemini API'ye istek gönderirken bir hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Sıcaklık bilgisi alınamadı.");
        }
    }

    // MGM Servisinden sıcaklık bilgisini al (Bu metodda değişiklik yok)
    private static String getTemperatureFromMGM() {
        io.restassured.response.Response response = RestAssured // RestAssured Response'u olduğunu belirtmek için tam yolu kullandık
                .given()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9,tr;q=0.8,bg;q=0.7")
                .header("Connection", "keep-alive")
                .header("Origin", "https://www.mgm.gov.tr")
                .header("Referer", "https://www.mgm.gov.tr/")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                .when()
                .get(WEATHER_URL)
                .then()
                .extract()
                .response();

        if (response.statusCode() == 200) {
            JSONArray dataArray = new JSONArray(response.getBody().asString());
            if (dataArray.length() > 0) {
                JSONObject firstItem = dataArray.getJSONObject(0);
                double sicaklikValue = firstItem.getDouble("sicaklik");
                return String.valueOf(sicaklikValue);            }
        }
        System.err.println("MGM'den sıcaklık alınamadı. Durum Kodu: " + response.statusCode());
        return null;
    }

    // Gemini API'ye prompt gönder
    private static void sendPromptToGemini(String temperature) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String promptText = "Şu anda İstanbul’da hava " + temperature + "°C. Sence dışarı çıkılır mı?";

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
    }
}