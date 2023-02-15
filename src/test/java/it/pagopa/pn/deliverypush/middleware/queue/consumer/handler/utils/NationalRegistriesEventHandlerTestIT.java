package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.deliverypush.MockSQSTest;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessage;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessageDigitalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.function.Consumer;

@FunctionalSpringBootTest
class NationalRegistriesEventHandlerTestIT extends MockSQSTest {

    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private PublicRegistryResponseHandler handler;

    @Test
    void consumeMessageOK() {
        Consumer<Message<AddressSQSMessage>> pnNationalRegistriesEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnNationalRegistriesEventInboundConsumer");
        AddressSQSMessage addressSQSMessage = new AddressSQSMessage()
                .correlationId("corr1")
                .taxId("taxId1")
                .digitalAddress(List.of(new AddressSQSMessageDigitalAddress().address("prova@pec.it").type("PEC")));
        Message<AddressSQSMessage> message = MessageBuilder.withPayload(addressSQSMessage).build();
        pnNationalRegistriesEventInboundConsumer.accept(message);
        Mockito.verify(handler).handleResponse(Mockito.any());
    }

    @Test
    void consumeMessageKO() {
        Consumer<Message<AddressSQSMessage>> pnNationalRegistriesEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnNationalRegistriesEventInboundConsumer");
        AddressSQSMessage addressSQSMessage = new AddressSQSMessage()
                .correlationId("corr1")
                .taxId("taxId1")
                .digitalAddress(List.of(new AddressSQSMessageDigitalAddress().address("prova@pec.it").type("PEC")));
        Message<AddressSQSMessage> message = MessageBuilder.withPayload(addressSQSMessage).build();
        Mockito.doThrow(new RuntimeException()).when(handler).handleResponse(Mockito.any());
        Assertions.assertThrows(RuntimeException.class,
                () -> pnNationalRegistriesEventInboundConsumer.accept(message));
    }


}
