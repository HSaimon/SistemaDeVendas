package com.example.SistemaDeVendas.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean de codificação de senha com BCrypt (strength padrão = 10 rounds).
     * Injetado em UsuarioApplication para codificar senhas no cadastro/atualização.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura o UserDetailsManager para buscar usuários e roles do banco MySQL.
     * A tabela 'roles' deve armazenar os valores com prefixo ROLE_
     * (ex: ROLE_ADMIN, ROLE_VENDEDOR, ROLE_CLIENTE).
     */
    @Bean
    public UserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery(
            "SELECT cpf, senha, enabled FROM usuario WHERE cpf = ?"
        );
        manager.setAuthoritiesByUsernameQuery(
            "SELECT u.cpf, r.role FROM usuario u " +
            "INNER JOIN roles r ON u.id = r.usuario_id " +
            "WHERE u.cpf = ?"
        );
        return manager;
    }

    /**
     * Regras de autorização por role:
     *  - ROLE_ADMIN    → acesso total a todos os endpoints
     *  - ROLE_VENDEDOR → cria e gerencia pedidos, pagamentos e itens
     *  - ROLE_CLIENTE  → leitura de pedidos, produtos e clientes
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth

            // Produtos: somente ADMIN gerencia; todos autenticados consultam
            .requestMatchers(HttpMethod.GET,    "/produto/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/produto/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/produto/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/produto/**").hasRole("ADMIN")

            // Pedidos: VENDEDOR e ADMIN criam/editam; autenticados consultam
            .requestMatchers(HttpMethod.GET,    "/pedido/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/pedido/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.PUT,    "/pedido/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.DELETE, "/pedido/**").hasRole("ADMIN")

            // Pagamentos: VENDEDOR e ADMIN registram
            .requestMatchers(HttpMethod.GET,    "/pagamento/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/pagamento/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.PUT,    "/pagamento/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.DELETE, "/pagamento/**").hasRole("ADMIN")

            // Clientes: leitura autenticada; escrita apenas ADMIN
            .requestMatchers(HttpMethod.GET,    "/cliente/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/cliente/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/cliente/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/cliente/**").hasRole("ADMIN")

            // Itens de pedido: VENDEDOR e ADMIN
            .requestMatchers(HttpMethod.GET,    "/itemPedido/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/itemPedido/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.PUT,    "/itemPedido/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.DELETE, "/itemPedido/**").hasAnyRole("ADMIN", "VENDEDOR")

            // Descontos: VENDEDOR e ADMIN consultam e gerenciam
            .requestMatchers(HttpMethod.GET,    "/descontoFidelidade/**").authenticated()
            .requestMatchers(HttpMethod.POST,   "/descontoFidelidade/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.PUT,    "/descontoFidelidade/**").hasAnyRole("ADMIN", "VENDEDOR")
            .requestMatchers(HttpMethod.DELETE, "/descontoFidelidade/**").hasRole("ADMIN")

            // Gestão administrativa — somente ADMIN
            .requestMatchers("/usuario/**").hasRole("ADMIN")
            .requestMatchers("/funcionario/**").hasRole("ADMIN")
            .requestMatchers("/cargo/**").hasRole("ADMIN")
            .requestMatchers("/tipoPagamento/**").hasRole("ADMIN")

            .anyRequest().authenticated()
        );

        http.httpBasic(Customizer.withDefaults());
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
