package uk.gov.justice.hmpps.datacompliance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.justice.hmpps.datacompliance.security.AuthAwareTokenConverter;
import uk.gov.justice.hmpps.datacompliance.web.controllers.RetentionController;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@Configuration
@EnableSwagger2
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
                        "/health", "/health/ping", "/info",
                        "/v2/api-docs",
                        "/swagger-resources/**",
                        "/swagger-ui.html",
                        "/webjars/**")
                        .permitAll().anyRequest().authenticated())
                .oauth2ResourceServer().jwt().jwtAuthenticationConverter(new AuthAwareTokenConverter());
    }

    @Bean
    public Docket api() {

        final var apiInfo = new ApiInfo(
                "DPS Data Compliance Service - API Documentation",
                "API for services regarding Data Compliance of Digital Prison Services.",
                getVersion(), "", contactInfo(), "", "", emptyList());

        final var docket = new Docket(SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo)
                .select()
                .apis(basePackage(RetentionController.class.getPackage().getName()))
                .paths(PathSelectors.any())
                .build();

        docket.genericModelSubstitutes(Optional.class);
        docket.directModelSubstitute(ZonedDateTime.class, java.util.Date.class);
        docket.directModelSubstitute(LocalDateTime.class, java.util.Date.class);

        return docket;
    }

    private String getVersion(){
        return buildProperties == null ? "version not available" : buildProperties.getVersion();
    }

    private Contact contactInfo() {
        return new Contact("HMPPS Digital Studio", "", "feedback@digital.justice.gov.uk");
    }
}
