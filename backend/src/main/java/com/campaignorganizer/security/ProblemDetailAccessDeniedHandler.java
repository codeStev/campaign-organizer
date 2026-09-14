package com.campaignorganizer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Same {@code application/problem+json} shape as {@code DomainExceptionAdvice}, but for
 * denials that never reach the DispatcherServlet — Spring Security's own authorization
 * filter throws {@link AccessDeniedException} before a controller method is invoked, so
 * {@code @RestControllerAdvice} (a Spring MVC concept) can't intercept it.
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final HttpStatus status;
    private final String title;
    private final String detail;
    private final ObjectMapper objectMapper;

    public ProblemDetailAccessDeniedHandler(HttpStatus status, String title, String detail,
                                            ObjectMapper objectMapper) {
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
