package com.example.springGroupBA.dto.fireStat;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FireStatDTO {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("currentCount") Integer currentCount,
            @JsonProperty("data") List<Item> data,
            @JsonProperty("matchCount") Integer matchCount,
            @JsonProperty("totalCount") Integer totalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonAlias({
                    "화재발생년월일", "일시", "화재발생일시",
                    "occrDt", "occr_dt", "date", "ymd"
            })
            String date,

            @JsonAlias({
                    "시도", "시도명", "지역",
                    "sido", "sidoNm", "sido_nm", "region"
            })
            String location,

            @JsonAlias({
                    "인명피해(명)소계", "인명피해소계",
                    "stotDmgPeopCnt", "dmgPeopCnt", "casualtyTotal"
            })
            String casualtyCount,

            @JsonAlias({
                    "사망", "사망자수",
                    "deathPeopCnt", "deathCnt", "death"
            })
            String deathCount,

            @JsonAlias({
                    "부상", "부상자수",
                    "injPeopCnt", "injuryCnt", "injury"
            })
            String injuryCount,

            @JsonAlias({
                    "재산피해소계",
                    "propDmg", "propertyDamage", "stotPropDmg"
            })
            String propertyDamage
    ) {
        @Override
        public String toString() {
            return "Item{" +
                    "date='" + date + '\'' +
                    ", location='" + location + '\'' +
                    ", casualty='" + casualtyCount + '\'' +
                    ", death='" + deathCount + '\'' +
                    ", injury='" + injuryCount + '\'' +
                    '}';
        }
    }
}