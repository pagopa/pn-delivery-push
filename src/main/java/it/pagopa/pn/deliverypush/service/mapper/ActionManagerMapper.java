package it.pagopa.pn.deliverypush.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ActionManagerMapper {
    private final ObjectMapper objectMapper;

    public NewAction fromActionInternalToActionDto(
            Action action) {
        return new NewAction()
                .actionId(action.getActionId())
                .iun(action.getIun())
                .notBefore(action.getNotBefore())
                .type(ActionType.valueOf(action.getType().name()))
                .recipientIndex(action.getRecipientIndex())
                .timelineId(action.getTimelineId())
                .details(getDetailsJson(action));
    }

    private String getDetailsJson(Action action) {
        if (action.getDetails() != null) {
            try {
                return objectMapper.writeValueAsString(action.getDetails());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Errore nella serializzazione JSON di ActionDetails", e);
            }
        }
        return "";
    }
}
