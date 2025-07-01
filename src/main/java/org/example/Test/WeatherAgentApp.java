package org.example.Test;

import org.example.layerDesign.Services.GeminiService;
import org.example.layerDesign.Services.WeatherServices;

public class WeatherAgentApp {
    public static void main(String[] args) {
        WeatherServices weatherServices = new WeatherServices();
        GeminiService geminiService = new GeminiService();

        String temperature = weatherServices.getTemperature();
        if (temperature != null) {
            String prompt = "Şu anda İstanbul’da hava " + temperature + "°C. Sence dışarı çıkılır mı?";
            try {
                String response = geminiService.askGemini(prompt);
                System.out.println("Gemini'nin cevabı: " + response);
            } catch (Exception e) {
                System.err.println("Gemini API hatası: " + e.getMessage());
            }
        } else {
            System.out.println("Sıcaklık bilgisi alınamadı.");
        }
    }
}
