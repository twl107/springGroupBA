package com.example.springGroupBA.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;



@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomRedirectException.class)
    public String handleCustomRedirectException(CustomRedirectException ex,
                                                RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", ex.getMessage());
        return "redirect:" + ex.getRedirectUrl();
    }


    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex,
                                              RedirectAttributes redirectAttributes,
                                              HttpServletRequest request) {

        redirectAttributes.addFlashAttribute("message", "접근 권한이 없습니다.");

        String requestURI = request.getRequestURI();

        if(requestURI.startsWith("/notice")) {
            return "redirect:/notice/noticeList";
        }
        else if (requestURI.startsWith("/board")) {
            return "redirect:/board/boardInput";
        }

        return "redirect:/";
    }
}
