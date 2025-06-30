package br.gov.sp.tce.icp_brasil_authenticator.domain.service;

import java.security.MessageDigest;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.springframework.stereotype.Service;

import br.gov.sp.tce.icp_brasil_authenticator.configuration.LoggingConfiguration;
import br.gov.sp.tce.icp_brasil_authenticator.domain.dto.CertificateDTO;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CertificateValidationService {
    
    private static final Pattern CPF_PATTERN = Pattern.compile("(\\d{11})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    
    public CertificateDTO validateCertificate(X509Certificate[] certificateChain, String remoteAddress, String userAgent) {
        LoggingConfiguration.TRANSACAO_LOG.info("Iniciando processamento de certificado digital");
        
        if (certificateChain == null || certificateChain.length == 0) {
            throw new IllegalArgumentException("Cadeia de certificados não fornecida");
        }
        
        X509Certificate clientCertificate = certificateChain[0];
        
        try {
            CertificateDTO certificateDTO = new CertificateDTO();
            
            // Extrair informações do certificado principal
            extractCertificateInfo(clientCertificate, certificateDTO);
            
            // Dados para auditoria
            certificateDTO.setFingerprint(generateSHA256Fingerprint(clientCertificate));
            certificateDTO.setSubjectDN(clientCertificate.getSubjectX500Principal().getName());
            certificateDTO.setIssuerDN(clientCertificate.getIssuerX500Principal().getName());
            certificateDTO.setValidationTimestamp(LocalDateTime.now());
            certificateDTO.setRemoteAddress(remoteAddress);
            certificateDTO.setUserAgent(userAgent);
            certificateDTO.setSessionId(UUID.randomUUID().toString());
            
            // Processar cadeia de certificados
            List<CertificateDTO.CertificateChainDTO> chain = new ArrayList<>();
            for (X509Certificate cert : certificateChain) {
                chain.add(createCertificateChainInfo(cert));
            }
            certificateDTO.setCertificateChain(chain);
            
            // Verificar apenas se o certificado está dentro do período de validade
            boolean isValid = isValidityPeriodValid(clientCertificate);
            certificateDTO.setValid(isValid);
            
            // Log de auditoria completo
            LoggingConfiguration.TRANSACAO_LOG.info(
                "LOGIN_CERTIFICADO|timestamp={}|ip={}|cpf={}|serialNumber={}|fingerprint={}|issuer={}|subject={}|validUntil={}|algorithm={}|sessionId={}|userAgent={}", 
                certificateDTO.getValidationTimestamp(),
                certificateDTO.getRemoteAddress(),
                certificateDTO.getCpf(),
                certificateDTO.getSerialNumber(),
                certificateDTO.getFingerprint(),
                certificateDTO.getIssuerDN(),
                certificateDTO.getSubjectDN(),
                certificateDTO.getNotAfter(),
                certificateDTO.getSignatureAlgorithm(),
                certificateDTO.getSessionId(),
                certificateDTO.getUserAgent()
            );
            
            return certificateDTO;
            
        } catch (Exception e) {
            LoggingConfiguration.TRANSACAO_LOG.error("Erro no processamento do certificado: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar certificado", e);
        }
    }
    
    private void extractCertificateInfo(X509Certificate certificate, CertificateDTO dto) {
        X500Principal subject = certificate.getSubjectX500Principal();
        X500Principal issuer = certificate.getIssuerX500Principal();
        
        dto.setSubjectName(extractCommonName(subject.getName()));
        dto.setIssuerName(extractCommonName(issuer.getName()));
        dto.setSerialNumber(certificate.getSerialNumber().toString());
        dto.setNotBefore(LocalDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.systemDefault()));
        dto.setNotAfter(LocalDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault()));
        dto.setSignatureAlgorithm(certificate.getSigAlgName());
        dto.setVersion(certificate.getVersion());
        
        // Extrair CPF do subject alternative name ou do subject
        String subjectStr = subject.getName();
        String cpf = extractCpf(subjectStr);
        dto.setCpf(cpf);
        
        // Extrair email
        String email = extractEmail(subjectStr);
        dto.setEmail(email);
    }
    
    private CertificateDTO.CertificateChainDTO createCertificateChainInfo(X509Certificate certificate) {
        X500Principal subject = certificate.getSubjectX500Principal();
        X500Principal issuer = certificate.getIssuerX500Principal();
        
        CertificateDTO.CertificateChainDTO chainInfo = new CertificateDTO.CertificateChainDTO();
        chainInfo.setSubjectName(extractCommonName(subject.getName()));
        chainInfo.setIssuerName(extractCommonName(issuer.getName()));
        chainInfo.setSerialNumber(certificate.getSerialNumber().toString());
        chainInfo.setNotBefore(LocalDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.systemDefault()));
        chainInfo.setNotAfter(LocalDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault()));
        chainInfo.setIsRoot(subject.equals(issuer));
        
        return chainInfo;
    }
    
    private boolean isValidityPeriodValid(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return true;
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            LoggingConfiguration.TRANSACAO_LOG.warn("Certificado fora do período de validade: {}", e.getMessage());
            return false;
        }
    }
    
    private String extractCommonName(String distinguishedName) {
        Pattern pattern = Pattern.compile("CN=([^,]+)");
        Matcher matcher = pattern.matcher(distinguishedName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return distinguishedName;
    }
    
    private String extractCpf(String subjectName) {
        Matcher matcher = CPF_PATTERN.matcher(subjectName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractEmail(String subjectName) {
        Matcher matcher = EMAIL_PATTERN.matcher(subjectName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String generateSHA256Fingerprint(X509Certificate certificate) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = certificate.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            LoggingConfiguration.TRANSACAO_LOG.error("Erro ao gerar fingerprint SHA-256: {}", e.getMessage());
            return "ERRO_FINGERPRINT";
        }
    }
}
