package uk.gov.justice.hmpps.datacompliance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.justice.hmpps.datacompliance.security.AuthAwareTokenConverter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig extends WebSecurityConfigurerAdapter {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
            .sessionManagement()
            .sessionCreationPolicy(STATELESS).and().csrf().disable()
            .authorizeRequests(auth -> auth.antMatchers(
                    "/health/**", "/info",
                    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                    "/webjars/**")
                .permitAll().anyRequest().authenticated())
            .oauth2ResourceServer().jwt().jwtAuthenticationConverter(new AuthAwareTokenConverter());
    }

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("DPS Data Compliance Service - API Documentation")
                .description("A service regarding Data Compliance of Digital Prison Services")
                .version(getVersion())
                .contact(contactInfo()));
    }

    private String getVersion() {
        return buildProperties == null ? "version not available" : buildProperties.getVersion();
    }

    private Contact contactInfo() {
        return new Contact().name("NOMIS Data Compliance").email("NOMISDataCompliance@justice.gov.uk");
    }
}
