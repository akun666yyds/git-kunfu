package com.gitkunfu.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request, HttpServletResponse response) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer value ? value : 500;
        String uri = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        String accept = request.getHeader("Accept");

        boolean browserPage = accept != null && accept.contains("text/html")
                && !uri.startsWith("/api/") && !uri.startsWith("/git/");
        if (status == HttpStatus.NOT_FOUND.value() && browserPage) {
            response.setStatus(HttpStatus.OK.value());
            return "forward:/index.html";
        }

        response.setStatus(status);
        String message = status == HttpStatus.NOT_FOUND.value()
                ? "请求的资源不存在"
                : "服务器开小差了，请稍后再试";
        return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    }
}
