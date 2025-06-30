package br.gov.sp.tce.icp_brasil_authenticator.domain.controller;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.gov.sp.tce.icp_brasil_authenticator.configuration.LoggingConfiguration;
import br.gov.sp.tce.icp_brasil_authenticator.domain.dto.CertificateDTO;
import br.gov.sp.tce.icp_brasil_authenticator.domain.service.CertificateValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
@Tag(name = "Certificate Validation", description = "API para validação de certificados digitais ICP-Brasil")
public class CertificateController {
    
    private final CertificateValidationService certificateValidationService;
    
    @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Processar certificado digital", 
               description = "Recebe uma requisição HTTPS com certificado digital e retorna os dados extraídos do certificado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Certificado processado com sucesso",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = CertificateDTO.class))),
        @ApiResponse(responseCode = "400", description = "Certificado não fornecido"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<CertificateDTO> validateCertificate(HttpServletRequest request) {
        LoggingConfiguration.ACESSO_LOG.info("Requisição de processamento de certificado recebida de: {}", 
            request.getRemoteAddr());
        
        try {
            // Obter certificados da requisição HTTPS
            X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
            
            if (certificates == null || certificates.length == 0) {
                LoggingConfiguration.ACESSO_LOG.warn("Nenhum certificado fornecido na requisição de: {}", 
                    request.getRemoteAddr());
                return ResponseEntity.badRequest().build();
            }
            
            // Processar certificado com dados da requisição
            String remoteAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            CertificateDTO certificateData = certificateValidationService.validateCertificate(
                certificates, remoteAddress, userAgent);
            
            LoggingConfiguration.ACESSO_LOG.info("Certificado processado com sucesso para: {} - Subject: {}", 
                request.getRemoteAddr(), certificateData.getSubjectName());
            
            return ResponseEntity.ok(certificateData);
            
        } catch (Exception e) {
            LoggingConfiguration.ACESSO_LOG.error("Erro no processamento do certificado para: {} - Erro: {}", 
                request.getRemoteAddr(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping(value = "/debug", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Debug de certificados", 
               description = "Endpoint para verificar se certificados estão sendo enviados")
    public ResponseEntity<Map<String, Object>> debugCertificate(HttpServletRequest request) {
        LoggingConfiguration.ACESSO_LOG.info("Debug de certificados solicitado de: {}", request.getRemoteAddr());
        
        Map<String, Object> debugInfo = new HashMap<>();
        
        // Verificar certificados
        X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        debugInfo.put("certificatesFound", certificates != null);
        debugInfo.put("certificateCount", certificates != null ? certificates.length : 0);
        
        // Log todos os atributos da requisição
        logRequestAttributes(request);
        
        // Informações da requisição
        debugInfo.put("remoteAddr", request.getRemoteAddr());
        debugInfo.put("scheme", request.getScheme());
        debugInfo.put("serverPort", request.getServerPort());
        debugInfo.put("isSecure", request.isSecure());
        
        return ResponseEntity.ok(debugInfo);
    }
    
    private void logRequestAttributes(HttpServletRequest request) {
        LoggingConfiguration.ACESSO_LOG.info("=== Debug de Atributos da Requisição ===");
        LoggingConfiguration.ACESSO_LOG.info("Scheme: {}", request.getScheme());
        LoggingConfiguration.ACESSO_LOG.info("Server Port: {}", request.getServerPort());
        LoggingConfiguration.ACESSO_LOG.info("Is Secure: {}", request.isSecure());
        LoggingConfiguration.ACESSO_LOG.info("Remote Addr: {}", request.getRemoteAddr());
        
        // Verificar atributos relacionados a certificados
        String[] certAttributes = {
            "jakarta.servlet.request.X509Certificate",
            "javax.servlet.request.X509Certificate",
            "jakarta.servlet.request.ssl_session_id",
            "javax.servlet.request.ssl_session_id"
        };
        
        for (String attr : certAttributes) {
            Object value = request.getAttribute(attr);
            LoggingConfiguration.ACESSO_LOG.info("Atributo {}: {}", attr, value != null ? "PRESENTE" : "AUSENTE");
            if (value instanceof X509Certificate[]) {
                X509Certificate[] certs = (X509Certificate[]) value;
                LoggingConfiguration.ACESSO_LOG.info("Número de certificados encontrados: {}", certs.length);
            }
        }
        LoggingConfiguration.ACESSO_LOG.info("=== Fim Debug ===");
    }
}
