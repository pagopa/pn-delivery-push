package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryIOSentMessageEvent;
import it.pagopa.pn.deliverypush.MockSQSTest;
import it.pagopa.pn.deliverypush.action.iosentmessage.IOSentMessageHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.function.Consumer;


@FunctionalSpringBootTest
class IOSentMessageEventHandlerTestIT extends MockSQSTest {

    @Autowired
    private FunctionCatalog functionCatalog;

    @MockBean
    private IOSentMessageHandler handler;

    @Test
    void pnExtRegistryIOSentMessageConsumer() {

        Instant instant = Instant.now();

        Consumer<Message<PnExtRegistryIOSentMessageEvent.Payload>> pnExtRegistryIOSentMessageConsumer =
                functionCatalog.lookup(Consumer.class, "pnExtRegistryIOSentMessageConsumer");
        PnExtRegistryIOSentMessageEvent.Payload body =PnExtRegistryIOSentMessageEvent.Payload.builder()
                .iun("IUN")
                .recIndex(1)
                .sendDate(instant)
                .build();

        Message<PnExtRegistryIOSentMessageEvent.Payload> message = MessageBuilder.withPayload(body).build();
        pnExtRegistryIOSentMessageConsumer.accept(message);
        Mockito.verify(handler).handleIOSentMessage("IUN", 1, instant);
    }

}