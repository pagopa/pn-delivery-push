package binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.binding.PnEventInboundService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PnEventInboundService.class
})
//@EnableBinding(MyProcessor.class)
@EnableAutoConfiguration(exclude = {CassandraAutoConfiguration.class})
class bindingTest {
    //    @Autowired
//    MyProcessor processor;
    @SpyBean
    @Autowired
    PnEventInboundService pnExtChnPaperEventInboundService;

    private static final ObjectMapper om;

    static {
        om = new ObjectMapper();
        JSR310Module javaTimeModule = new JSR310Module();
        om.registerModule(javaTimeModule);
    }

    /*
        @Test
        void whenSendMessage_thenResponseShouldUpdateText() {
            HashMap<String, Object> headers = new HashMap<>();
            headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventType.SEND_PAPER_REQUEST.name());
            headers.put(PN_EVENT_HEADER_EVENT_ID, "123");
            headers.put(PN_EVENT_HEADER_PUBLISHER, "pub");
            headers.put(PN_EVENT_HEADER_IUN, "iun");
            headers.put(PN_EVENT_HEADER_CREATED_AT, Instant.now().toString());
            PnDeliveryNewNotificationEvent.Payload event = mockPaperMessage().getPayload(); // should discard
            String payload = toJson("test");
            GenericMessage<String> message = new GenericMessage<>(payload, headers);
            //SubscribableChannel channel = processor.myinput();
            //channel.send(message);
        }
        */
    public static String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    public static PnDeliveryNewNotificationEvent mockPaperMessage() {
        return PnDeliveryNewNotificationEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun("IUN")
                        .build()
                )
                .payload(PnDeliveryNewNotificationEvent.Payload.builder()
                        .paId("PA_ID")
                        .build()
                )
                .build();

    }

}
