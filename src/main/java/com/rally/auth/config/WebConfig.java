package com.rally.auth.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path storageRoot;

    public WebConfig(@Value("${app.storage.root:./uploads}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storageRoot.toUri().toString();
        if (location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
