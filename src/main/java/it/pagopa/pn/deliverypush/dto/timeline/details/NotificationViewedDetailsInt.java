package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder( toBuilder = true )
@EqualsAndHashCode
@ToString
public class NotificationViewedDetailsInt implements RecipientRelatedTimelineElementDetails, PersonalInformationRelatedTimelineElement{
    private int recIndex;
    private Integer notificationCost;
    private String raddType;
    private String raddTransactionId;
    private DelegateInfoInt delegateInfo;
    
    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
    
    @Override
    public String getTaxId() {
        return delegateInfo != null ? delegateInfo.getTaxId() : null;
    }

    @Override
    public void setTaxId(String taxId) {
        if(delegateInfo != null){
            delegateInfo.setTaxId(taxId);
        }
    }

    @Override
    public String getDenomination() {
        return delegateInfo != null ? delegateInfo.getDenomination() : null;
    }

    @Override
    public void setDenomination(String denomination) {
        if(delegateInfo != null){
            delegateInfo.setDenomination(denomination);
        }
    }
}
