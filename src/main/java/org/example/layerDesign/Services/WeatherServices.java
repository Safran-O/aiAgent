package org.example.layerDesign.Services;

import io.restassured.RestAssured;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherServices {
    private static final String WEATHER_URL = "https://servis.mgm.gov.tr/web/sondurumlar?merkezid=93401";

    public String getTemperature() {
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
}
