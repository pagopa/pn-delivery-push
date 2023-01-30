package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.pn.deliverypush.action.details.NotHandledDetails;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "actionType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "ANALOG_WORKFLOW"),
        @JsonSubTypes.Type(value = RecipientsWorkflowDetails.class, name = "START_RECIPIENT_WORKFLOW"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "CHOOSE_DELIVERY_MODE"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "ANALOG_WORKFLOW"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_NEXT_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_RETRY_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "REFINEMENT_NOTIFICATION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "SENDER_ACK"),
        @JsonSubTypes.Type(value = DocumentCreationResponseActionDetails.class, name = "DOCUMENT_CREATION_RESPONSE")
})
public interface ActionDetails {

}
