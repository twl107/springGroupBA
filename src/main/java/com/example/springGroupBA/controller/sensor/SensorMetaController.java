package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.service.sensor.SensorMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-meta")
@RequiredArgsConstructor
public class SensorMetaController {

  private final SensorMetaService service;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/delete/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
