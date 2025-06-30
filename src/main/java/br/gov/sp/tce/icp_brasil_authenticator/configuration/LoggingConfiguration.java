package br.gov.sp.tce.icp_brasil_authenticator.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {
    
    // Logger para acesso
    public static final Logger ACESSO_LOG = LoggerFactory.getLogger("br.gov.sp.tce.icp_brasil_authenticator.ACESSO");
    
    // Logger para transações
    public static final Logger TRANSACAO_LOG = LoggerFactory.getLogger("br.gov.sp.tce.icp_brasil_authenticator.TRANSACAO");
}
