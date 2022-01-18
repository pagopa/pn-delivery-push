package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.webhook.configuration.ClientCertificateCfg;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class WebhookClientCertImplTest {

    private WebhookClientCertImpl clientCert;

    private ClientCertificateCfg certCfg;

    @BeforeEach
    void setup() {
        this.certCfg = Mockito.mock( ClientCertificateCfg.class );
        clientCert = new WebhookClientCertImpl( );
    }

    //@Test //da eseguire in locale dopo aver lanciato un'instanza mockserver
    void sendInfoWithCertSuccess() {
        //Given
        String url = "https://localhost:1080/test";
        List<WebhookOutputDto> data = new ArrayList<>();
        data.add( WebhookOutputDto.builder()
                        .iun( "IUN" )
                        .senderId( "SENDER_ID" )
                        .notificationElement("NOTIFICATION_ELEMENT")
                .build() );

        //When
        Mockito.when( certCfg.getClientCertificatePem() )
                .thenReturn( loadMockClientCert() );
        Mockito.when( certCfg.getClientKeyPem() )
                .thenReturn( loadMockClientKey() );

        clientCert.sendInfo( url, data, certCfg);

        //Then
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


        //When
        clientCert.sendInfo( url, data, certCfg);

        //Then
    }

    static String loadINPSCert() {
        try {
            return Files.readString(Paths.get( "file:///Users/alessandromasci/Desktop/mockserver/INPS.pem" ));
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
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
