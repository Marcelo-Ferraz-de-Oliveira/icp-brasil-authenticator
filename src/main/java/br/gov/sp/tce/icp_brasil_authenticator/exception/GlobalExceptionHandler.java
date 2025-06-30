package br.gov.sp.tce.icp_brasil_authenticator.exception;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import br.gov.sp.tce.icp_brasil_authenticator.configuration.LoggingConfiguration;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        LoggingConfiguration.ACESSO_LOG.info("Erro de argumento inválido - URI: {} - Mensagem: {}", 
            request.getDescription(false), ex.getMessage());
        
        ErrorDTO errorDTO = new ErrorDTO(Collections.singletonList(
            new ErrorDTO.Message(ex.getMessage())
        ));
        
        return ResponseEntity.badRequest().body(errorDTO);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDTO> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        LoggingConfiguration.ACESSO_LOG.error("Erro de runtime - URI: {} - Mensagem: {}", 
            request.getDescription(false), ex.getMessage());
        
        ErrorDTO errorDTO = new ErrorDTO(Collections.singletonList(
            new ErrorDTO.Message("Erro interno na validação do certificado")
        ));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGeneralException(
            Exception ex, WebRequest request) {
        
        LoggingConfiguration.ACESSO_LOG.error("Erro geral - URI: {} - Mensagem: {}", 
            request.getDescription(false), ex.getMessage());
        
        ErrorDTO errorDTO = new ErrorDTO(Collections.singletonList(
            new ErrorDTO.Message("Erro interno do servidor")
        ));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDTO);
    }
}
