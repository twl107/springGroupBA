package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.dto.sensor.DailySensorDto;
import com.example.springGroupBA.service.sensor.DailyReportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class DailyReportController {

  private final DailyReportService reportService;

  private static final String SERVICE_KEY = "7WMGwpEENfXvFnxY1efwZ4263gPHczyuehE7RyufhGeO4SZPOKxDisyWglB%2BjylPIXZJu8Xxs8BCWVbLqr9PdA%3D%3D";

  @GetMapping("/daily")
  public List<DailySensorDto> getDaily(
          @RequestParam String date,
          @RequestParam String time,
          @RequestParam String device
  ) {
    LocalDate d = LocalDate.parse(date);
    LocalTime t = LocalTime.parse(time);

    return reportService.getDailyReport(d, t, device);
  }

  @ResponseBody
  @PostMapping(value="/weather/daily", produces="application/json; charset=UTF-8")
  public String getDailyWeather(String date, int areaNo) throws Exception {

    String year = date.substring(0, 4);
    String month = date.substring(5, 7);

    StringBuilder urlBuilder = new StringBuilder(
            "http://apis.data.go.kr/1360000/SfcMtlyInfoService/getDailyWthrData"
    );

    urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + SERVICE_KEY);
    urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=1");
    urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=10");
    urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=JSON");
    urlBuilder.append("&" + URLEncoder.encode("year","UTF-8") + "=" + year);
    urlBuilder.append("&" + URLEncoder.encode("month","UTF-8") + "=" + month);
    urlBuilder.append("&" + URLEncoder.encode("station","UTF-8") + "=" + areaNo);

    URL url = new URL(urlBuilder.toString());
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Content-type", "application/json");

    BufferedReader rd;
    if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
      rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    } else {
      rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
    }

    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = rd.readLine()) != null) {
      sb.append(line);
    }

    rd.close();
    conn.disconnect();

    if (sb.toString().contains("resultCode\":\"99")) {
      return "{\"error\": \"해당 날짜는 기상정보가 제공되지 않습니다.\"}";
    }

    // JSON 파싱
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree(sb.toString());

    JsonNode items = json
            .path("response")
            .path("body")
            .path("items")
            .path("item")
            .path(0)
            .path("stndays")
            .path("info");

    // 하루에 여러 정보가 있으므로 리스트로 반환
    List<Map<String, String>> result = new ArrayList<>();

    for (JsonNode node : items) {
      Map<String, String> map = new HashMap<>();
      map.put("tm", node.path("tm").asText());
      map.put("temp", node.path("ta").asText());
      map.put("tempMin", node.path("ta_min").asText());
      map.put("tempMax", node.path("ta_max").asText());
      map.put("humidity", node.path("hm").asText());
      map.put("wind", node.path("ws").asText());
      String rainStr = node.path("rn_day").asText();
      map.put("rain", (rainStr.equals("null") || rainStr.isBlank()) ? "0" : rainStr);
      result.add(map);
    }

    return mapper.writeValueAsString(result);
  }


}
