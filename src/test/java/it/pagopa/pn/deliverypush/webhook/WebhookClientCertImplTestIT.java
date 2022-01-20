package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.utils.ssl.SSLContextFactory;
import it.pagopa.pn.deliverypush.webhook.configuration.ClientCertificateCfg;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


class WebhookClientCertImplTestIT {

    private ClientCertificateCfg certCfg;

    private RestTemplate restTemplate;

    private WebhookClientCertImpl clientCert;

    private SSLContextFactory sslContextFactory;

    @BeforeEach
    void setup() {
        this.certCfg = Mockito.mock( ClientCertificateCfg.class );
        this.sslContextFactory = new SSLContextFactory();
    }

    @Test //da eseguire in locale dopo aver lanciato un'instanza mockserver
    void sendInfoWithCertSuccess() {
        //Given
        String url = "https://localhost:1080/webhook/";
        List<WebhookOutputDto> data = new ArrayList<>();
        data.add( WebhookOutputDto.builder()
                        .iun( "IUN" )
                        .senderId( "SENDER_ID" )
                        .notificationElement("NOTIFICATION_ELEMENT")
                .build() );
        Mockito.when( certCfg.getClientCertificatePem() )
                .thenReturn( loadMockClientCert() );
        Mockito.when( certCfg.getClientKeyPem() )
                .thenReturn( loadMockClientKey() );
        Mockito.when( certCfg.getServerCertificatesPem() )
                .thenReturn( Collections.singletonList( loadMockServerCert() ));

        RestTemplate rt = initialize();

        //When
        clientCert.sendInfo( url, data);

        //Then
        Mockito.verify( rt ).exchange( url, HttpMethod.POST, new HttpEntity<>(data, null), Void.class );
    }

    private RestTemplate initialize() {
        restTemplate = new WebhookRestTemplateFactory( this.certCfg, sslContextFactory).restTemplate();
        RestTemplate rt = Mockito.spy( restTemplate );
        clientCert = new WebhookClientCertImpl(rt);
        return rt;
    }

    @Test //da eseguire in locale dopo aver lanciato un'instanza mockserver
    void sendInfoNoCertSuccess() {
        //Given
        String url = "https://pn-status-webhook.free.beeceptor.com/test";
        List<WebhookOutputDto> data = new ArrayList<>();
        data.add( WebhookOutputDto.builder()
                .iun( "IUN" )
                .senderId( "SENDER_ID" )
                .notificationElement("NOTIFICATION_ELEMENT")
                .build() );

        RestTemplate rt = initialize();

        //When
        clientCert.sendInfo( url, data);

        //Then
        Mockito.verify( rt ).exchange( url, HttpMethod.POST, new HttpEntity<>(data, null), Void.class );
    }

    static String loadMockClientCert() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("/Users/alessandromasci/Desktop/mockserver/mockserver.client.pem"));
            return String.join("", lines.subList( 1, lines.size() - 1 ));
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    static String loadMockClientKey() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("/Users/alessandromasci/Desktop/mockserver/mockserver.client.key8.pem"));
            return String.join("", lines.subList( 1, lines.size() - 1 ));
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    static String loadMockServerCert() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("/Users/alessandromasci/Desktop/mockserver/CertificateAuthorityCertificate.pem"));
            return String.join("", lines.subList( 1, lines.size() - 1 ));
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
}
