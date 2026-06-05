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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map the /uploads/** URL to the physical C:/uploads/ directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:C:/uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activeSessionInterceptor)
                .addPathPatterns("/student/**", "/api/**")
                .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**");
    }
}
 