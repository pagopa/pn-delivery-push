package it.pagopa.pn.deliverypush.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt.AAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class ActionManagerMapperTest {
    @MockBean
    private ObjectMapper objectMapper;

    @Test
    void fromActionInternalToActionDto_shouldMapFieldsAndSerializeDetails() {

        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("key1")
                .documentCreationType(AAR)
                .timelineId("timeline1")
                .build();

        Action action = Action.builder()
                .actionId("id1")
                .iun("iun1")
                .notBefore(Instant.now())
                .type(ActionType.ANALOG_WORKFLOW)
                .recipientIndex(2)
                .details(details)
                .timelineId("timeline1")
                .build();

        ActionManagerMapper mapper = new ActionManagerMapper(objectMapper);
        var dto = mapper.fromActionInternalToActionDto(action);

        assertEquals("id1", dto.getActionId());
        assertEquals("iun1", dto.getIun());
        assertEquals("timeline1", dto.getTimelineId());
        assertEquals(2, dto.getRecipientIndex());
        assertEquals("ANALOG_WORKFLOW", dto.getType().name());
    }

    @Test
    void fromActionInternalToActionDto_shouldHandleNullDetails() {
        Action action = Action.builder()
                .actionId("id2")
                .type(ActionType.ANALOG_WORKFLOW)
                .build();

        ActionManagerMapper mapper = new ActionManagerMapper(objectMapper);
        var dto = mapper.fromActionInternalToActionDto(action);

        assertEquals("", dto.getDetails());
    }
}
