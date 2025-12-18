package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.fireStat.FireStat;
import com.example.springGroupBA.dto.fireStat.FireStatDTO;
import com.example.springGroupBA.repository.fireStat.FireStatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FireStatService {

    private final FireStatRepository fireStatRepository;
    private final ObjectMapper objectMapper;

    @Value("${api.public-data.base-url}")
    private String baseUrl;

    @Value("${api.public-data.key}")
    private String apiKey;

    private final Map<Integer, String> apiEndpoints = Map.of(
            2024, "/15060386/v1/uddi:fa73f7a3-dfa1-4b0a-ada8-dcd8333ba9e4",
            2023, "/15060386/v1/uddi:9951ec3f-d1c9-49e8-9ed4-f026c39a7925",
            2022, "/15060386/v1/uddi:cb73d6d5-064c-4dd2-a136-8c3069aa1fe2",
            2021, "/15060386/v1/uddi:dd407ff5-f23a-4d46-b90b-dc37505fb02c",
            2020, "/15060386/v1/uddi:bd8a7575-d4c9-4a22-a972-fac12348dd7e"
    );

    @Transactional(readOnly = true)
    public List<FireStat> getStatsByYear(Integer year) {
        return fireStatRepository.findAllByYearOrderByLocationAsc(year);
    }

    public void syncApiDataForYear(Integer targetYear) {
        String endPoint = apiEndpoints.get(targetYear);

        if (endPoint == null) {
            log.warn("지원하지 않는 연도입니다: {}", targetYear);
            return;
        }

        log.info("=== {}년도 데이터 동기화 시작 ===", targetYear);
        RestClient restClient = RestClient.create();

        final int PER_PAGE = 1000;
        final int MAX_PAGE_CHECK = 100;
        final int BATCH_SIZE = 5;

        Map<String, FireStat> statMap = new ConcurrentHashMap<>();
        AtomicInteger totalCount = new AtomicInteger(0);
        boolean isEndReached = false;

        for (int startPage = 1; startPage <= MAX_PAGE_CHECK; startPage += BATCH_SIZE) {
            if (isEndReached) break;

            int endPage = Math.min(startPage + BATCH_SIZE - 1, MAX_PAGE_CHECK);

            boolean batchHasData = IntStream.rangeClosed(startPage, endPage).parallel().mapToObj(page -> {
                boolean hasData = false;
                for (int i = 0; i < 3; i++) {
                    try {
                        FireStatDTO.Response response = fetchPageData(restClient, endPoint, page, PER_PAGE);
                        if (response != null && response.data() != null && !response.data().isEmpty()) {
                            for (FireStatDTO.Item item : response.data()) {
                                aggregateItem(statMap, item, targetYear);
                            }
                            totalCount.addAndGet(response.data().size());
                            hasData = true;
                        }
                        break;
                    } catch (Exception e) {
                        try { Thread.sleep(500); } catch (Exception ex) {}
                    }
                }
                return hasData;
            }).reduce(false, (a, b) -> a || b);

            if (!batchHasData) {
                isEndReached = true;
            }
            try { Thread.sleep(500); } catch (Exception e) {}
        }

        if (!statMap.isEmpty()) {
            saveToDb(statMap, targetYear);
            log.info("{}년도 동기화 완료! (수집된 데이터: {}건)", targetYear, totalCount.get());
        } else {
            log.warn("{}년도 수집된 데이터가 없습니다. API 상태를 확인하세요.", targetYear);
        }
    }

    private void aggregateItem(Map<String, FireStat> statMap, FireStatDTO.Item item, Integer targetYear) {
        String dateStr = item.date();
        if (dateStr == null || dateStr.length() < 4) return;

        try {
            String yearStr = dateStr.substring(0, 4);
            int itemYear = Integer.parseInt(yearStr);
            if (itemYear != targetYear.intValue()) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }

        String location = item.location();
        if (location == null || location.isBlank()) return;

        int totalCasualty = parseToInt(item.casualtyCount());
        int death = parseToInt(item.deathCount());
        int injury = parseToInt(item.injuryCount());

        if (totalCasualty == 0 && (death > 0 || injury > 0)) {
            totalCasualty = death + injury;
        }

        final int finalCasualty = totalCasualty;

        statMap.compute(location, (k, v) -> {
            if (v == null) {
                return FireStat.builder()
                        .year(targetYear)
                        .location(location)
                        .fireCount(1)
                        .casualtyCount(finalCasualty)
                        .build();
            }
            return FireStat.builder()
                    .id(v.getId())
                    .year(targetYear)
                    .location(location)
                    .fireCount(v.getFireCount() + 1)
                    .casualtyCount(v.getCasualtyCount() + finalCasualty)
                    .build();
        });
    }

    private FireStatDTO.Response fetchPageData(RestClient client, String endPoint, int page, int perPage) throws Exception {
        URI uri = createUri(endPoint, page, perPage);
        String rawJson = client.get().uri(uri).retrieve().body(String.class);
        return objectMapper.readValue(rawJson, FireStatDTO.Response.class);
    }

    private URI createUri(String endPoint, int page, int perPage) {
        String urlString = String.format("%s%s?page=%d&perPage=%d&serviceKey=%s",
                baseUrl, endPoint, page, perPage, apiKey);
        return URI.create(urlString);
    }

    private void saveToDb(Map<String, FireStat> statMap, Integer year) {
        if (fireStatRepository.existsByYear(year)) {
            fireStatRepository.deleteByYear(year);
        }
        fireStatRepository.saveAll(statMap.values());
    }

    private int parseToInt(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLong(String val) {
        if (val == null || val.isBlank()) return 0L;
        try {
            return Long.parseLong(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}