package br.gov.sp.tce.icp_brasil_authenticator.exception;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDTO {
    private List<Message> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String message;
    }
}
