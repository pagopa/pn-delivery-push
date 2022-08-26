package it.pagopa.pn.deliverypush.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class GlobalErrorHandlerTest {

    //ServerWebExchange exchange = MockServerHttpRequest.get("/").toExchange();

    @Mock
    private ObjectMapper objectMapper;

    private GlobalErrorHandler globalErrorHandler;

    @BeforeEach
    public void setup() {
        globalErrorHandler = new GlobalErrorHandler(objectMapper);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handle() {

        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost")
                .header("X-CF-Forwarded-Url", "https://example.com").build();
        ServerWebExchange serverWebExchange = MockServerWebExchange.from(request);
        
        Throwable throwable = Mockito.notNull();

        Mockito.when(globalErrorHandler.handle(serverWebExchange, throwable)).thenReturn(Mono.empty());
        //globalErrorHandler.handle(Mockito.any(), Mockito.any());

        Mockito.verify(globalErrorHandler).handle(Mockito.any(ServerWebExchange.class), Mockito.any(Throwable.class));
    }
}