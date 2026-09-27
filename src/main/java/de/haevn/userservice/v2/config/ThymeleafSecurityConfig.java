package de.haevn.userservice.v2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

@Configuration
public class ThymeleafSecurityConfig {

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        // Diese Bean sorgt dafür, dass Thymeleaf die "sec:authorize"-Attribute versteht und auswertet
        return new SpringSecurityDialect();
    }
}