package com.company.inventario.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuracion de seguridad — soporta DOS metodos de autenticacion:
 *
 *  1. API Key  (header X-Service-Api-Key)
 *     → para service-to-service y pruebas rapidas sin Keycloak
 *
 *  2. JWT Bearer Token emitido por Keycloak
 *     → para clientes externos (Postman, apps web/mobile)
 *     → roles extraidos del claim realm_access.roles
 *
 * PROBLEMA DOCKER SPLIT-NETWORK (causa del 401):
 * ─────────────────────────────────────────────────
 *  Postman llama Keycloak en → http://localhost:8180
 *    el token JWT tiene: iss = "http://localhost:8180/realms/microservicios"
 *
 *  El microservicio (dentro de Docker) solo alcanza Keycloak en → http://keycloak:8080
 *    Si usaramos spring.security.oauth2.resourceserver.jwt.issuer-uri,
 *    Spring haria OIDC discovery desde keycloak:8080 y encontraria
 *    issuer="http://keycloak:8080/..." != iss del token → 401
 *
 * SOLUCION: ReactiveJwtDecoder manual con dos URLs separadas:
 *   jwk-set-uri    = http://keycloak:8080/.../certs  (URL interna: descarga las claves)
 *   jwt-issuer-uri = http://localhost:8180/...        (URL publica: valida claim 'iss')
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.security.api-key}")
    private String validApiKey;

    @Value("${app.security.api-key-header}")
    private String apiKeyHeader;

    /** URL interna Docker para descargar JWKS de Keycloak */
    @Value("${app.security.jwk-set-uri:}")
    private String jwkSetUri;

    /** URL publica que coincide con el claim 'iss' del token emitido para Postman */
    @Value("${app.security.jwt-issuer-uri:}")
    private String jwtIssuerUri;

    // ──────────────────────────────────────────────────────────────────────────
    // FILTER CHAIN
    // ──────────────────────────────────────────────────────────────────────────
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeExchange(ex -> ex
                        .pathMatchers(
                                "/actuator/health", "/actuator/health/**",
                                "/actuator/prometheus",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(apiKeyAuthFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .oauth2ResourceServer(oauth2 -> {
                    if (jwkSetUri != null && !jwkSetUri.isBlank()) {
                        // Perfil docker/prod: usar decoder custom con URL interna para JWKS
                        // y URL publica para validar el claim 'iss' del token
                        oauth2.jwt(jwt -> jwt
                                .jwtDecoder(buildJwtDecoder())
                                .jwtAuthenticationConverter(keycloakJwtConverter())
                        );
                    } else {
                        // Perfil local sin Keycloak: JWT desactivado, solo API Key funciona
                        oauth2.jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtConverter())
                        );
                    }
                    oauth2.authenticationEntryPoint(
                            new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED));
                })
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API KEY FILTER
    // ──────────────────────────────────────────────────────────────────────────
    @Bean
    public WebFilter apiKeyAuthFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (isPublicPath(path)) return chain.filter(exchange);

            String key = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);
            if (key != null && key.equals(validApiKey)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "service-account", null,
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                );
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }
            // No hay API Key valida → dejar pasar al JWT filter
            return chain.filter(exchange);
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // JWT DECODER CUSTOM (resuelve el split-network de Docker)
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Construye un ReactiveJwtDecoder que:
     *  - Descarga JWKS desde URL INTERNA Docker (http://keycloak:8080/.../certs)
     *  - Valida el claim 'iss' contra la URL PUBLICA (http://localhost:8180/...)
     *
     * Sin este decoder: iss del token = "localhost:8180"
     *                   issuer de Spring = "keycloak:8080" → MISMATCH → 401
     */
    private ReactiveJwtDecoder buildJwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());          // valida exp y nbf
        if (jwtIssuerUri != null && !jwtIssuerUri.isBlank()) {
            validators.add(new JwtIssuerValidator(jwtIssuerUri)); // valida claim 'iss'
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // KEYCLOAK ROLES CONVERTER
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Keycloak incluye los roles en: realm_access.roles
     *   { "realm_access": { "roles": ["ROLE_USER", "offline_access"] } }
     *
     * Spring Security por defecto los busca en 'scope' o 'scp' → no los encuentra.
     * Este converter los extrae del lugar correcto.
     */
    @Bean
    public ReactiveJwtAuthenticationConverter keycloakJwtConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakRolesConverter());
        return converter;
    }

    private Converter<Jwt, Flux<GrantedAuthority>> keycloakRolesConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Flux.empty();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            List<GrantedAuthority> authorities = roles.stream()
                    .filter(r -> !r.equals("offline_access")
                              && !r.equals("uma_authorization")
                              && !r.startsWith("default-roles-"))
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return Flux.fromIterable(authorities);
        };
    }


    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/actuator/info")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars");
    }
}
