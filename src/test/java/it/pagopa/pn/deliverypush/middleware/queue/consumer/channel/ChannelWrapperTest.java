package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


class ChannelWrapperTest {
    @Test
    void withMDCPropagatesMdcKeysFromHeaders() {
        Message<String> message = createMessage(new HashMap<>(Map.of(
                "aws_messageId", "aws-id-123",
                "X-Amzn-Trace-Id", "trace-abc",
                "iun", "iun-xyz"
        )));
    
        AtomicBoolean called = new AtomicBoolean(false);
    
        Consumer<Message<String>> consumer = msg -> {
            called.set(true);
            Assertions.assertEquals("aws-id-123", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
            Assertions.assertEquals("trace-abc", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
            Assertions.assertEquals("iun-xyz", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        };
    
        ChannelWrapper.withMDC(consumer).accept(message);
        Assertions.assertTrue(called.get());
    }
    
    @Test
    void withMDCGeneratesTraceIdIfMissing() {
        Message<String> message = createMessage(new HashMap<>());
    
        Consumer<Message<String>> consumer = msg -> Assertions.assertNotNull(MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
    
        ChannelWrapper.withMDC(consumer).accept(message);
    }

    private Message<String> createMessage(HashMap<String, Object> headers) {
        MessageHeaders messageHeaders = new MessageHeaders(headers);
        return new Message<>() {
            @Override
            public @NotNull String getPayload() {
                return "payload";
            }

            @Override
            public @NotNull MessageHeaders getHeaders() {
                return messageHeaders;
            }
        };
    }
}