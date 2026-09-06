package gr.hua.dit.ap.vmp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // <-- Ενεργοποίηση method security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Δημόσιες σελίδες
                        .requestMatchers("/", "/register", "/registration-pending").permitAll()
                        .requestMatchers("/volunteer/register").permitAll()
                        .requestMatchers("/organization/register").permitAll()
                        .requestMatchers("/organization/user/register").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Μόνο για ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Για volunteer και admin
                        .requestMatchers("/volunteer/**").hasAnyRole("VOLUNTEER", "ADMIN")

                        // Για οργανισμούς και admin
                        .requestMatchers("/organization/**").hasAnyRole("ORGANIZATION", "ADMIN")

                        // Συμμετοχές
                        .requestMatchers("/participation/**").hasAnyRole("VOLUNTEER", "ORGANIZATION", "ADMIN")

                        // Αξιολογήσεις
                        .requestMatchers("/review/**").hasAnyRole("VOLUNTEER", "ORGANIZATION", "ADMIN")

                        // Εκδηλώσεις: λίστα για όλους τους ρόλους
                        .requestMatchers("/event/list").hasAnyRole("ORGANIZATION", "ADMIN", "VOLUNTEER")

                        // Διαχείριση εκδηλώσεων μόνο οργανισμός & admin
                        .requestMatchers("/event/new", "/event/edit/**", "/event/cancel/**", "/event/delete/**")
                        .hasAnyRole("ORGANIZATION", "ADMIN")

                        // Άλλα event endpoints για όλους (π.χ. λεπτομέρειες)
                        .requestMatchers("/event/**").hasAnyRole("ORGANIZATION", "ADMIN", "VOLUNTEER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}