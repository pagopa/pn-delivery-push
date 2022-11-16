package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.common;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseClient extends CommonBaseClient {

    protected BaseClient( ){
    }

    protected WebClient.Builder initWebClient(WebClient.Builder builder){

        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS)));

        return super.enrichBuilder(builder.clientConnector(new ReactorClientHttpConnector(httpClient)));
    }

}
