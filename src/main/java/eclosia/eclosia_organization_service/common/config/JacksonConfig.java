package eclosia.eclosia_organization_service.common.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eclosia.eclosia_organization_service.common.jackson.StatusBooleanDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Module statusBooleanModule() {
        SimpleModule module = new SimpleModule();
        StatusBooleanDeserializer deserializer = new StatusBooleanDeserializer();
        module.addDeserializer(Boolean.class, deserializer);
        module.addDeserializer(Boolean.TYPE, deserializer);
        return module;
    }
}
