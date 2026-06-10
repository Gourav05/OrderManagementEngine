package com.peerisland.orderengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI orderEngineOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Order Management Engine API")
                .version("v1")
                .description("REST API for managing orders and status transitions")
                .contact(new Contact().name("Peerisland")));
    }
}
