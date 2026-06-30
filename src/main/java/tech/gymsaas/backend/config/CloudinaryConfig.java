package tech.gymsaas.backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    // Automatically reads CLOUDINARY_CLOUD_NAME from your environment variables
    @Value("${CLOUDINARY_CLOUD_NAME}")
    private String cloudName;

    // Automatically reads CLOUDINARY_API_KEY from your environment variables
    @Value("${CLOUDINARY_API_KEY}")
    private String apiKey;

    // Automatically reads CLOUDINARY_API_SECRET from your environment variables
    @Value("${CLOUDINARY_API_SECRET}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}