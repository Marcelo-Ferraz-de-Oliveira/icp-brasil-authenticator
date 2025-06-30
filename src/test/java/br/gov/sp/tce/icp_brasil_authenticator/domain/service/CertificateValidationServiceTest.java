package br.gov.sp.tce.icp_brasil_authenticator.domain.service;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import br.gov.sp.tce.icp_brasil_authenticator.domain.dto.CertificateDTO;

@ExtendWith(MockitoExtension.class)
class CertificateValidationServiceTest {

    @InjectMocks
    private CertificateValidationService certificateValidationService;

    @Test
    void testValidateCertificate_WithValidCertificate_ShouldReturnCertificateDTO() {
        // Given
        X509Certificate mockCertificate = mock(X509Certificate.class);
        X509Certificate[] certificateChain = {mockCertificate};
        
        // Mock certificate data
        when(mockCertificate.getSubjectX500Principal())
            .thenReturn(new javax.security.auth.x500.X500Principal("CN=João da Silva:12345678901, O=Test"));
        when(mockCertificate.getIssuerX500Principal())
            .thenReturn(new javax.security.auth.x500.X500Principal("CN=AC SOLUTI, O=Test CA"));
        when(mockCertificate.getSerialNumber()).thenReturn(new BigInteger("123456789"));
        when(mockCertificate.getNotBefore()).thenReturn(new Date());
        when(mockCertificate.getNotAfter()).thenReturn(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
        when(mockCertificate.getSigAlgName()).thenReturn("SHA256withRSA");
        when(mockCertificate.getVersion()).thenReturn(3);
        
        // Mock checkValidity to not throw exception (valid certificate)
        try {
            doNothing().when(mockCertificate).checkValidity();
        } catch (Exception e) {
            // Not expected in test
        }

        // When
        CertificateDTO result = certificateValidationService.validateCertificate(certificateChain, "192.168.1.1", "Mozilla/5.0");

        // Then
        assertNotNull(result);
        assertEquals("João da Silva:12345678901", result.getSubjectName());
        assertEquals("AC SOLUTI", result.getIssuerName());
        assertEquals("123456789", result.getSerialNumber());
        assertEquals("SHA256withRSA", result.getSignatureAlgorithm());
        assertEquals(3, result.getVersion());
        assertTrue(result.getValid());
        assertNotNull(result.getCertificateChain());
        assertEquals(1, result.getCertificateChain().size());
    }

    @Test
    void testValidateCertificate_WithNullChain_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> certificateValidationService.validateCertificate(null, "192.168.1.1", "Mozilla/5.0")
        );
        
        assertEquals("Cadeia de certificados não fornecida", exception.getMessage());
    }

    @Test
    void testValidateCertificate_WithEmptyChain_ShouldThrowException() {
        // Given
        X509Certificate[] emptyChain = {};

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> certificateValidationService.validateCertificate(emptyChain, "192.168.1.1", "Mozilla/5.0")
        );
        
        assertEquals("Cadeia de certificados não fornecida", exception.getMessage());
    }
}
