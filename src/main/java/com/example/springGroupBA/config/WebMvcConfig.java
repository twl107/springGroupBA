package com.example.springGroupBA.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Value("${org.zerock.upload.path}")
  private String uploadPath;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {

    registry.addResourceHandler("/upload/board/**")
            .addResourceLocations("file:///" + uploadPath + "board/");

    registry.addResourceHandler("/upload/review/**")
            .addResourceLocations("file:///" + uploadPath + "review/");

    registry.addResourceHandler("/upload/resume/**")
            .addResourceLocations("file:///" + uploadPath + "resume/");

    registry.addResourceHandler("/upload/pds/**")
            .addResourceLocations("file:///" + uploadPath + "pds/");

    registry.addResourceHandler("/upload/notice/**")
            .addResourceLocations("file:///" + uploadPath + "notice/");

    registry.addResourceHandler("/upload/cktemp/**")
            .addResourceLocations("file:///" + uploadPath + "cktemp/");

    registry.addResourceHandler("/upload/shop/**")
            .addResourceLocations("file:///" + uploadPath + "shop/");

    registry.addResourceHandler("/upload/gallery/**")
            .addResourceLocations("file:///" + uploadPath + "gallery/");

    registry.addResourceHandler("/upload/inquiry/**")
            .addResourceLocations("file:///" + uploadPath + "inquiry/");

    registry.addResourceHandler("/upload/sensor/**")
            .addResourceLocations("file:///" + uploadPath + "sensor/");

    registry.addResourceHandler("/upload/member/**")
            .addResourceLocations("file:///" + uploadPath + "member/")
            .setCachePeriod(0)
            .resourceChain(true)
            .addResolver(new PathResourceResolver());
  }

}
