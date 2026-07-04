package eclosia.eclosia_organization_service.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eclosia Organization Service API")
                        .description("API de gestion organisationnelle Eclosia")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Eclosia")
                                .email("support@eclosia.com")))
                .servers(List.of(
                        new Server()
                                .url(contextPath.isBlank() ? "/" : contextPath)
                                .description("Serveur local")
                ));
    }
}
