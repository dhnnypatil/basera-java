package com.basera.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves photos uploaded to the local `uploads` folder at /uploads/... (when Cloudinary isn't configured).
 *
 * Files in src/main/resources/static are served automatically (express.static("public")), but those are
 * packed inside the jar and can't change while the app runs. Uploads are saved outside the jar,
 * so they need this extra mapping: app.use("/uploads", express.static("uploads")).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${basera.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().toUri().toString(); // e.g. file:///C:/.../uploads/
        registry.addResourceHandler("/uploads/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
