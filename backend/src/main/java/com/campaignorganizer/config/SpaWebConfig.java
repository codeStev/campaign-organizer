package com.campaignorganizer.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the SPA shell (index.html) for client-side routes with no matching
 * static file — e.g. a hard refresh on /worlds/1/articles/2 — on the
 * combined image that bundles the frontend build into static/ (ADR-0059).
 * A no-op on the API-only image: static/index.html isn't on the classpath
 * there, so the fallback resource simply doesn't exist and requests 404
 * as normal.
 *
 * Registered as a low-priority resource handler, so /api/**, actuator, and
 * Swagger UI (all backed by @RequestMapping controllers or their own
 * higher-priority handler mappings) are resolved first and never reach it.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaIndexFallbackResolver());
    }

    private static final class SpaIndexFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            return resource != null ? resource : new ClassPathResource("static/index.html");
        }
    }
}
