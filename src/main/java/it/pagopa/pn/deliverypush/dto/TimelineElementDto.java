package it.pagopa.pn.deliverypush.dto;


import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import org.springframework.beans.BeanUtils;

public class TimelineElementDto {
    
    /*
    public Object getSpecificDetails(){
        TimelineElementCategory category = this.getCategory();
        
        switch (category){
            case SCHEDULE_ANALOG_WORKFLOW:
                return new ScheduleAnalogWorkflow();XW
        }
        throw new UnsupportedOperationException();
    }
    
     */
    
    public static void setSpecificDetails(TimelineElement timelineElement, Object specificDetails){
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        BeanUtils.copyProperties(specificDetails, timelineElementDetails);
        timelineElement.setDetails(timelineElementDetails);
    }
        
}
