package project.moonki.config; // 패키지명은 프로젝트에 맞게

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        System.out.println("WebConfig.addCorsMappings()");
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173") // 프론트엔드 주소로 변경
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
        registry.addMapping("/auth/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
