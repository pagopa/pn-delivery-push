package it.pagopa.pn.deliverypush.webhook.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties( prefix = "cert")
@Data
public class CertCfg {

    private String clientCertificatePem;
    private String clientKeyPem;
    private List<String> trustedServerCertificates;
}
