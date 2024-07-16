package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.legalfacts.AarTemplateType;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AarCreationRequestDetailsInt implements RecipientRelatedTimelineElementDetails{
    private int recIndex;
    private String aarKey;
    private Integer numberOfPages; //Nota il campo potrà essere eliminato in futuro dal momento che il numero di pagine viene calcolato da paperChannel
    private AarTemplateType aarTemplateType;
    
    public String toLog() {
        return String.format(
                "recIndex=%d aarKey=%s numberOfPages=%s aarTemplateType=%s",
                recIndex,
                aarKey,
                numberOfPages,
                aarTemplateType
        );
    }
}
