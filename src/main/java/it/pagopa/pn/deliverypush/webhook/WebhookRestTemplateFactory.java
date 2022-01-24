package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.utils.ssl.SSLContextFactory;
import it.pagopa.pn.deliverypush.webhook.configuration.ClientCertificateCfg;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.util.List;

@Slf4j
@Configuration
public class WebhookRestTemplateFactory {


    public static final String REST_TEMPLATE_WITH_CLIENT_CERTIFICATE = "webhook_rest_template";

    private final ClientCertificateCfg certCfg;

    private final SSLContextFactory sslContextFactory;

    public WebhookRestTemplateFactory(ClientCertificateCfg certCfg, SSLContextFactory sslContextFactory) {
        this.certCfg = certCfg;
        this.sslContextFactory = sslContextFactory;
    }

    @Bean
    @Qualifier(REST_TEMPLATE_WITH_CLIENT_CERTIFICATE)
    public RestTemplate restTemplate() {
        RestTemplate result = new RestTemplate();

        String clientCertificatePem = certCfg.getClientCertificatePem();
        String clientKeyPem = certCfg.getClientKeyPem();
        List<String> serverCertificatesPem = certCfg.getServerCertificatesPem();

        if (checkCfg( clientCertificatePem, clientKeyPem, serverCertificatesPem )) {
            HttpClient client = buildSSLHttpClient(clientCertificatePem, clientKeyPem, serverCertificatesPem);
            result.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
        } else {
            String message = clientCertificatePem == null || clientCertificatePem.isEmpty() ?
                    "clientCertificatePem" : "clientKeyPem";
            log.warn("Unable to retrieve " + message + " from environment variables");
        }
        return result;
    }

    private boolean checkCfg(String clientCertificatePem, String clientKeyPem, List<String> trustedServerCertificates) {
        if( clientCertificatePem == null || clientCertificatePem.isEmpty() ) return false;
        if( clientKeyPem == null || clientKeyPem.isEmpty() ) return false;
        return !trustedServerCertificates.isEmpty();
    }

    private HttpClient buildSSLHttpClient(String clientCertificatePem, String clientKeyPem, List<String> trustedServerCertificates) {
        SSLContext sslContext = sslContextFactory.buildSSLHttpClient( clientCertificatePem, clientKeyPem, trustedServerCertificates );
        return HttpClients.custom().setSSLContext(sslContext).build();
    }






}