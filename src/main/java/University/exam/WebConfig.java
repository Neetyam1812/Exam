package University.exam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import University.exam.interceptor.ActiveSessionInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActiveSessionInterceptor activeSessionInterceptor;

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:C:/uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir;
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location += "/";
        }
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeSessionInterceptor)
                .addPathPatterns("/student/**", "/api/**")
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**");
    }
}
 