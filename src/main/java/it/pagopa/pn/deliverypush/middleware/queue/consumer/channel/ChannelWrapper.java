package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;
import java.util.function.Consumer;

public class ChannelWrapper {
    /**
     * Wraps a consumer to set MDC context based on the message headers.
     * @param consumer Consumer<Message<T>> to be executed with MDC context set
     * @return Consumer<Message<T>> that sets MDC context before executing the provided consumer
     * @param <T> Type of the message payload
     */
    public static <T> Consumer<Message<T>> withMDC(Consumer<Message<T>> consumer) {
        return message -> {
            setMdc(message);
            consumer.accept(message);
        };
    }

    private static void setMdc(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        MDCUtils.clearMDCKeys();

        if (messageHeaders.containsKey("aws_messageId")){
            String awsMessageId = messageHeaders.get("aws_messageId", String.class);
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (messageHeaders.containsKey("X-Amzn-Trace-Id")){
            String traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }

        String iun = (String) message.getHeaders().get("iun");
        if(iun != null){
            MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
        }
    }
}
