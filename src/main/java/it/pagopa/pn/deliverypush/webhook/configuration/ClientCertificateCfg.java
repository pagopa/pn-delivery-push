package it.pagopa.pn.deliverypush.webhook.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties( prefix = "cert")
@Data
public class ClientCertificateCfg {

    private String clientCertificatePem;
    private String clientKeyPem;
}
