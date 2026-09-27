package de.haevn.userservice.v2.service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtils {

    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "SYSTEM"; // Fallback für asynchrone Tasks/Scheduler ohne HTTP-Kontext
        }

        HttpServletRequest request = attributes.getRequest();
        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }

        // Wenn der Header mehrere IPs enthält (Client, Proxy1, Proxy2), nimm die erste
        return xfHeader.split(",")[0].trim();
    }
}