package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.webhook.configuration.ClientCertificateCfg;
import it.pagopa.pn.deliverypush.webhook.dto.WebhookOutputDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class WebhookClientCertImpl implements WebhookClient{

    private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();

    private RestTemplate buildRestTemplate( ClientCertificateCfg certCfg ) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        SSLContext sslContext = null;
        try {
            String clientCertificatePem = certCfg.getClientCertificatePem();
            String clientKeyPem = certCfg.getClientKeyPem();

            if ( clientCertificatePem != null && !clientCertificatePem.isEmpty() && clientKeyPem != null && !clientKeyPem.isEmpty() ) {
                X509Certificate clientCert = buildX509CertificateFromPemString(clientCertificatePem);

                KeyStore keyStore = newKeyStore();

                keyStore.setCertificateEntry( "client_cert", clientCert );
                keyStore.setKeyEntry("client_key", buildKeyFromPemString(clientKeyPem),
                        KEYSTORE_PASSWORD,
                        new Certificate[] {clientCert});

                TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
                sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial( null, acceptingTrustStrategy )
                        .loadKeyMaterial(keyStore, KEYSTORE_PASSWORD)
                        .build();

                HttpClient client = HttpClients.custom().setSSLContext(sslContext).build();
                return builder
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                        .build();
            } else {
                String message = clientCertificatePem == null || clientCertificatePem.isEmpty()?
                        "clientCertificatePem" : "clientKeyPem";
                log.info( "Unable to retrieve " + message + " from environment variables" );
                return builder.build();
            }
        } catch (KeyStoreException | CertificateException |
                NoSuchAlgorithmException | IOException | UnrecoverableKeyException
                | KeyManagementException e) {
            throw new PnInternalException( e.getMessage(), e );
        }
    }

    private X509Certificate buildX509CertificateFromPemString(String stringCert) throws CertificateException {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(stringCert));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(is);
    }

    private PrivateKey buildKeyFromPemString(String clientKeyPem) {
        InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(clientKeyPem));
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec( is.readAllBytes() );
            return keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
            throw new PnInternalException("Error building private key from client key pem", e );
        }
    }

    private KeyStore newKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, KEYSTORE_PASSWORD);

        OutputStream outputStream = OutputStream.nullOutputStream();
        ks.store( outputStream, KEYSTORE_PASSWORD );

        return ks;
    }

    @Override
    public void sendInfo(String url, List<WebhookOutputDto> data, ClientCertificateCfg certCfg) {
        RestTemplate restTemplate = buildRestTemplate(certCfg);
        log.info("Send info webhook with url: " + url);
        HttpEntity<List<WebhookOutputDto>> entity = new HttpEntity<>(data, null);
        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        log.info("Response: " + resp);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new PnInternalException("Http error " + resp.getStatusCodeValue() + " calling webhook " + url);
        }
    }
}
