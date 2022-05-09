package it.pagopa.pn.deliverypush.util;


import ReceivedDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;

import java.util.HashMap;
import java.util.Map;

public class TimelineDetailMap {
    
    private static final Map<TimelineElementCategory, Class<?>> detailsMap = new HashMap<>();
    
    static {
        detailsMap.put( TimelineElementCategory.REQUEST_ACCEPTED, ReceivedDetails.class);//TODO Da analizzare ReceivedDetails
        detailsMap.put( TimelineElementCategory.SEND_COURTESY_MESSAGE, SendCourtesyMessageDetails.class );
        detailsMap.put( TimelineElementCategory.NOTIFICATION_PATH_CHOOSE, NotificationPathChooseDetails.class );
        detailsMap.put( TimelineElementCategory.GET_ADDRESS, GetAddressInfo.class );
        detailsMap.put( TimelineElementCategory.PUBLIC_REGISTRY_CALL, PublicRegistryCallDetails.class );
        detailsMap.put( TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, PublicRegistryResponseDetails.class );
        detailsMap.put( TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, ScheduleAnalogWorkflow.class );
        detailsMap.put( TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, ScheduleDigitalWorkflow.class );
        detailsMap.put( TimelineElementCategory.SEND_DIGITAL_DOMICILE, SendDigitalDetails.class );
        detailsMap.put( TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK, SendDigitalFeedbackDetails.class );
        detailsMap.put( TimelineElementCategory.SEND_DIGITAL_FEEDBACK, SendDigitalFeedback.class );
        detailsMap.put( TimelineElementCategory.SEND_DIGITAL_DOMICILE_FAILURE, SendDigitalFailureDetails.class );
        detailsMap.put( TimelineElementCategory.REFINEMENT, RefinementDetails.class );
        detailsMap.put( TimelineElementCategory.SCHEDULE_REFINEMENT, ScheduleRefinement.class );
        detailsMap.put( TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, DigitalSuccessWorkflow.class );
        detailsMap.put( TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, DigitalFailureWorkflow.class );
        detailsMap.put( TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, AnalogSuccessWorkflow.class );
        detailsMap.put( TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, AnalogFailureWorkflow.class );
        detailsMap.put( TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, SimpleRegisteredLetterDetails.class );
        detailsMap.put( TimelineElementCategory.END_OF_DIGITAL_DELIVERY_WORKFLOW, EndOfDigitalDeliveryWorkflowDetails.class );
        detailsMap.put( TimelineElementCategory.END_OF_ANALOG_DELIVERY_WORKFLOW, EndOfAnalogDeliveryWorkflowDetails.class );
        detailsMap.put( TimelineElementCategory.NOTIFICATION_VIEWED, NotificationViewedDetails.class );
        detailsMap.put( TimelineElementCategory.SEND_ANALOG_DOMICILE, SendPaperDetails.class );
        detailsMap.put( TimelineElementCategory.SEND_PAPER_FEEDBACK, SendPaperFeedbackDetails.class );
        detailsMap.put( TimelineElementCategory.PAYMENT, null );
        detailsMap.put( TimelineElementCategory.COMPLETELY_UNREACHABLE, CompletelyUnreachableDetails.class );
        detailsMap.put( TimelineElementCategory.REQUEST_REFUSED, RequestRefusedDetails.class );
    }
    
    public static Class<?> getDetailJavaClass(TimelineElementCategory category){
        return detailsMap.get(category);
    }

}
