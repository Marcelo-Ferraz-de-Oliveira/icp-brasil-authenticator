package br.gov.sp.tce.icp_brasil_authenticator.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!test")
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors
                .configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.addAllowedOrigin("*");
                    corsConfig.addAllowedMethod("*");
                    corsConfig.addAllowedHeader("*");
                    return corsConfig;
                })
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**", "/api/health/**").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterAfter(new CertificateLoggingFilter(), X509AuthenticationFilter.class);
        
        return http.build();
    }
    
    private static class CertificateLoggingFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
            
            // Captura o certificado cliente da requisição
            X509Certificate[] certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
            
            if (certs != null && certs.length > 0) {
                X509Certificate clientCert = certs[0];
                logCertificateInfo(clientCert);
            } else {
                log.info("Nenhum certificado cliente encontrado na requisição");
            }
            
            filterChain.doFilter(request, response);
        }
        
        private void logCertificateInfo(X509Certificate cert) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                
                log.info("=== INFORMAÇÕES DO CERTIFICADO CLIENTE ===");
                log.info("Subject (Titular): {}", cert.getSubjectX500Principal().getName());
                log.info("Issuer (Emissor): {}", cert.getIssuerX500Principal().getName());
                log.info("Serial Number: {}", cert.getSerialNumber());
                log.info("Válido de: {}", sdf.format(cert.getNotBefore()));
                log.info("Válido até: {}", sdf.format(cert.getNotAfter()));
                log.info("Versão: {}", cert.getVersion());
                log.info("Algoritmo de Assinatura: {}", cert.getSigAlgName());
                
                // Extrai informações específicas do Subject
                String subjectDN = cert.getSubjectX500Principal().getName();
                String cn = extractFromDN(subjectDN, "CN");
                String cpf = extractCPFFromCN(cn);
                String email = extractFromDN(subjectDN, "EMAILADDRESS");
                
                if (cn != null) log.info("Nome (CN): {}", cn);
                if (cpf != null) log.info("CPF extraído: {}", cpf);
                if (email != null) log.info("Email: {}", email);
                
                log.info("===============================================");
                
            } catch (Exception e) {
                log.error("Erro ao extrair informações do certificado: {}", e.getMessage());
            }
        }
        
        private String extractFromDN(String dn, String attribute) {
            String[] parts = dn.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith(attribute + "=")) {
                    return part.substring(attribute.length() + 1);
                }
            }
            return null;
        }
        
        private String extractCPFFromCN(String cn) {
            if (cn == null) return null;
            
            // Padrão típico: "NOME:CPF" ou "NOME CPF:XXXXX"
            if (cn.contains(":")) {
                String[] parts = cn.split(":");
                for (String part : parts) {
                    part = part.trim();
                    if (part.matches("\\d{11}")) {
                        return part;
                    }
                }
            }
            return null;
        }
    }
}
