package com.example.springGroupBA.controller;

import com.example.springGroupBA.entity.fireStat.FireStat;
import com.example.springGroupBA.service.FireStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/fireStat")
public class FireStatController {

    private final FireStatService fireStatService;

    @GetMapping("/fireStatList")
    public String index() {
        return "fireStat/fireStatList";
    }

    @PostMapping("/search")
    public String search(@RequestParam("year") Integer year, Model model) {
        fireStatService.syncApiDataForYear(year);

        List<FireStat> stats = fireStatService.getStatsByYear(year);

        int totalFireCount = stats.stream()
                .mapToInt(stat -> stat.getFireCount() != null ? stat.getFireCount() : 0)
                .sum();

        int totalCasualtyCount = stats.stream()
                .mapToInt(stat -> stat.getCasualtyCount() != null ? stat.getCasualtyCount() : 0)
                .sum();

        model.addAttribute("selectedYear", year);
        model.addAttribute("stats", stats);
        model.addAttribute("totalFireCount", totalFireCount);
        model.addAttribute("totalCasualtyCount", totalCasualtyCount);

        return "fireStat/fireStatList";
    }
}
