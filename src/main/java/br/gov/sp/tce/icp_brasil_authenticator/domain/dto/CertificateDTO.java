package br.gov.sp.tce.icp_brasil_authenticator.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados do certificado digital ICP-Brasil")
public class CertificateDTO {
    
    @Schema(description = "Nome do titular do certificado", example = "João da Silva")
    private String subjectName;
    
    @Schema(description = "CPF do titular", example = "12345678901")
    private String cpf;
    
    @Schema(description = "Email do titular", example = "joao.silva@email.com")
    private String email;
    
    @Schema(description = "Emissor do certificado", example = "AC SOLUTI")
    private String issuerName;
    
    @Schema(description = "Número de série do certificado")
    private String serialNumber;
    
    @Schema(description = "Data de início da validade")
    private LocalDateTime notBefore;
    
    @Schema(description = "Data de fim da validade")
    private LocalDateTime notAfter;
    
    @Schema(description = "Indica se o certificado é válido")
    private Boolean valid;
    
    @Schema(description = "Cadeia de certificados")
    private List<CertificateChainDTO> certificateChain;
    
    @Schema(description = "Algoritmo de assinatura")
    private String signatureAlgorithm;
    
    @Schema(description = "Versão do certificado")
    private Integer version;
    
    // Campos para auditoria
    @Schema(description = "Fingerprint SHA-256 do certificado (hash único)")
    private String fingerprint;
    
    @Schema(description = "Subject DN completo do certificado")
    private String subjectDN;
    
    @Schema(description = "Issuer DN completo do certificado")
    private String issuerDN;
    
    @Schema(description = "Timestamp do momento da validação")
    private LocalDateTime validationTimestamp;
    
    @Schema(description = "Endereço IP de origem da requisição")
    private String remoteAddress;
    
    @Schema(description = "User-Agent do navegador")
    private String userAgent;
    
    @Schema(description = "ID da sessão gerado")
    private String sessionId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Informações de um certificado na cadeia")
    public static class CertificateChainDTO {
        
        @Schema(description = "Nome do subject do certificado")
        private String subjectName;
        
        @Schema(description = "Nome do emissor do certificado")
        private String issuerName;
        
        @Schema(description = "Número de série")
        private String serialNumber;
        
        @Schema(description = "Data de início da validade")
        private LocalDateTime notBefore;
        
        @Schema(description = "Data de fim da validade")
        private LocalDateTime notAfter;
        
        @Schema(description = "Indica se é um certificado raiz")
        private Boolean isRoot;
    }
}
