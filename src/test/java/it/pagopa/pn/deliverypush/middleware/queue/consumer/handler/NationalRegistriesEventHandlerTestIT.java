package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.MockActionPoolTest;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.*;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.function.Consumer;

@FunctionalSpringBootTest
@Import(LocalStackTestConfig.class)
class NationalRegistriesEventHandlerTestIT extends MockActionPoolTest {

    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private NationalRegistriesResponseHandler handler;

    @MockBean
    private NotificationValidationActionHandler notificationValidationActionHandler;

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

    @Test
    void validationConsumeMessageOK() {
        Consumer<Message<AddressesSQSMessage>> pnNationalRegistriesValidationEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnNationalRegistriesValidationEventInboundConsumer");
        AddressesSQSMessage addressesSQSMessage = new AddressesSQSMessage()
                .correlationId("correlationId")
                .addresses(getAddressesSQSMessage());
        Message<AddressesSQSMessage> message = MessageBuilder.withPayload(addressesSQSMessage).build();
        pnNationalRegistriesValidationEventInboundConsumer.accept(message);
        Mockito.verify(notificationValidationActionHandler).handleValidateNationalRegistriesResponse(Mockito.eq("correlationId"), Mockito.anyList());
    }

    @Test
    void validationConsumeMessageKO() {
        Consumer<Message<AddressesSQSMessage>> pnNationalRegistriesValidationEventInboundConsumer = functionCatalog.lookup(Consumer.class, "pnNationalRegistriesValidationEventInboundConsumer");
        AddressesSQSMessage addressesSQSMessage = new AddressesSQSMessage()
                .correlationId("correlationId")
                .addresses(getAddressesSQSMessage());
        Message<AddressesSQSMessage> message = MessageBuilder.withPayload(addressesSQSMessage).build();
        Mockito.doThrow(new RuntimeException()).when(notificationValidationActionHandler).handleValidateNationalRegistriesResponse(Mockito.eq("correlationId"), Mockito.anyList());
        Assertions.assertThrows(RuntimeException.class,
                () -> pnNationalRegistriesValidationEventInboundConsumer.accept(message));
    }

    private static List<PhysicalAddressSQSMessage> getAddressesSQSMessage() {
        return List.of(new PhysicalAddressSQSMessage()
                .recIndex("1")
                .taxId("taxId1")
                .registry("ANPR")
                .physicalAddress(new PhysicalAddress()
                        .address("address")
                        .zip("zip")
                        .province("province")
                        .addressDetails("addressDetails")
                        .municipality("municipality")
                        .municipalityDetails("municipalityDetails")
                        .at("at")
                        .foreignState("foreignState")));
    }

}
