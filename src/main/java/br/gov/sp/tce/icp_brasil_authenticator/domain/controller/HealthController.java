package br.gov.sp.tce.icp_brasil_authenticator.domain.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoints para verificação de saúde da aplicação")
public class HealthController {

    @GetMapping("/status")
    @Operation(summary = "Verificar status da aplicação", 
               description = "Endpoint simples para verificar se a aplicação está funcionando")
    @ApiResponse(responseCode = "200", description = "Aplicação funcionando corretamente")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "message", "ICP-Brasil Authenticator está funcionando",
            "ssl", "Habilitado na porta 8443"
        ));
    }
}
