package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.entity.sensor.SensorMeta;
import com.example.springGroupBA.service.sensor.SensorMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/sensor")
@RequiredArgsConstructor
public class SensorInfoController {

  private final SensorMetaService sensorMetaService;

  @GetMapping("/info")
  public String sensorInfo(Model model, Authentication auth) {

    boolean isAdmin = auth != null && auth.getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    model.addAttribute("isAdmin", isAdmin);
    model.addAttribute("sensorList", sensorMetaService.findAll());

    return "sensor/sensorInfo";
  }

  @GetMapping("/detail/{code}")
  public String detail(@PathVariable String code, Model model) {

    SensorMeta meta = sensorMetaService.findByCode(code);
    if (meta == null) {
      return "sensor/detail/detail-notfound";
    }

    model.addAttribute("meta", meta);
    return "sensor/detail/detail";
  }

  /* ===== ADD ===== */
  @GetMapping("/add")
  public String addForm(Model model) {
    model.addAttribute("meta", new SensorMeta());
    return "sensor/detail/edit";
  }

  /* ===== EDIT ===== */
  @GetMapping("/edit/{id}")
  public String edit(@PathVariable Long id, Model model) {
    SensorMeta meta = sensorMetaService.findById(id).orElseThrow();
    model.addAttribute("meta", meta);
    return "sensor/detail/edit";
  }

  /* ===== SAVE (ADD + EDIT 공용) ===== */
  @PostMapping("/save")
  public String save(
          @ModelAttribute SensorMeta meta,
          @RequestParam(required = false) MultipartFile image
  ) throws IOException {

    sensorMetaService.saveWithImage(meta, image);
    return "redirect:/sensor/info";
  }
}

