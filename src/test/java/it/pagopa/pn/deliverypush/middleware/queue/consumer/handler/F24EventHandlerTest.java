package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.AsyncF24Event;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.f24.model.MetadataValidationEndEvent;
import it.pagopa.pn.deliverypush.middleware.responsehandler.F24ResponseHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;


@FunctionalSpringBootTest
@Import(LocalStackTestConfig.class)
class F24EventHandlerTest {

    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private F24ResponseHandler handler;

    @Test
    void consumeMessageOK() {
        Consumer<Message<AsyncF24Event>> pnF24EventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnF24EventInboundConsumer");

        AsyncF24Event asyncF24Event = new AsyncF24Event();
        MetadataValidationEndEvent metadataValidationEndEvent = new MetadataValidationEndEvent();
        metadataValidationEndEvent.setId("iun");
        metadataValidationEndEvent.setStatus("ok");
        asyncF24Event.setMetadataValidationEnd(metadataValidationEndEvent);

        Message<AsyncF24Event> message = MessageBuilder.withPayload(asyncF24Event).build();
        pnF24EventInboundConsumer.accept(message);
        Mockito.verify(handler).handleResponseReceived(Mockito.any());
    }
}
