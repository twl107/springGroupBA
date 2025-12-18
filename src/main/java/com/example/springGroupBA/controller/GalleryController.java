package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.gallery.GalleryDTO;
import com.example.springGroupBA.service.GalleryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/gallery")
@RequiredArgsConstructor
@Slf4j
public class GalleryController {

    private final GalleryService galleryService;

    @GetMapping("/galleryList")
    public String galleryListGet() {
        return "gallery/galleryList";
    }

    @GetMapping("/api/galleryList")
    @ResponseBody
    public Page<GalleryDTO> galleryListGet(@RequestParam(value = "page", defaultValue = "1") int page,
                                           @RequestParam(value = "size", defaultValue = "16") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return galleryService.getList(pageable);
    }

    @PostMapping("/register")
    public String register(GalleryDTO dto,
                           @RequestParam("file")MultipartFile file,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        try {
            galleryService.register(dto, file, authentication.getName());
            redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");
        }
        catch (Exception e) {
            log.error("Register Error", e);
            redirectAttributes.addFlashAttribute("message", "등록 중 오류가 발생했습니다.");
        }
        return "redirect:/gallery/galleryList";
    }

    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(GalleryDTO dto,
                                                      @RequestParam(value = "file", required = false) MultipartFile file,
                                                      Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            galleryService.modify(dto, file, authentication.getName(), isAdmin);
            return ResponseEntity.ok(Map.of("result", "success", "message", "수정되었습니다."));
        }
        catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "권한이 없습니다."));
        }
        catch (Exception e) {
            log.error("Update Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "수정 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            galleryService.delete(id, authentication.getName(), isAdmin);
            return ResponseEntity.ok(Map.of("result", "success"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "권한이 없습니다."));
        } catch (Exception e) {
            log.error("Delete Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "삭제 중 오류가 발생했습니다."));
        }
    }
}
