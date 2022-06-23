package it.pagopa.pn.deliverypush.middleware.externalclient;

import it.pagopa.pn.commons.exceptions.PnHttpClientResponseException;
import it.pagopa.pn.commons.exceptions.PnHttpServerResponseException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@Slf4j
@Component
public class RestTemplateResponseErrorHandler
        implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse)
            throws IOException {
        return (httpResponse.getStatusCode().series() == CLIENT_ERROR
                        || httpResponse.getStatusCode().series() == SERVER_ERROR);
    }

    @Override
    public void handleError(@NotNull ClientHttpResponse response) throws IOException {
        log.error("Error in call response={} ", response);
        
        if (response.getStatusCode().series() == SERVER_ERROR) {
            throw new PnHttpServerResponseException( "Error in rest request, status code "+ response.getStatusCode() );
        } else if (response.getStatusCode().series() == CLIENT_ERROR) {
            throw new PnHttpClientResponseException( "Error in rest request, status code "+ response.getStatusCode() );
        }
    }

    @Override
    public void handleError(@NotNull URI url, @NotNull HttpMethod method, ClientHttpResponse response)
            throws IOException {
        //TODO Gestire le differenti casistiche di errore, si potrebbe pensare di gestire in maniera differente la exception che dipendono dal client e quelle che dipendono dal server

        String body = null;
        try (InputStream responseBody = response.getBody()) {
            body = StreamUtils.copyToString(responseBody, StandardCharsets.UTF_8);
        }

        log.error("Error in call url={} method={} status code={} body={}", url, method, response.getStatusCode(), body);
        
        if (response.getStatusCode().series() == SERVER_ERROR) {
            throw new PnHttpServerResponseException("Error in call "+ url + " status code "+ response.getStatusCode() );
        } else if (response.getStatusCode().series() == CLIENT_ERROR) {
            throw new PnHttpClientResponseException("Error in call "+ url + " status code "+ response.getStatusCode() );
        }
    }
}