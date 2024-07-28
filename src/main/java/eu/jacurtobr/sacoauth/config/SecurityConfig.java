package eu.jacurtobr.sacoauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth/**")
                .permitAll()
                .anyRequest()
                .authenticated()
        )
                .formLogin( formLogin -> formLogin
                        .loginPage("/login")
                        .permitAll()
                );
        return http.build();
    }

}