package it.pagopa.pn.deliverypush.dto.timeline.details;

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
    private Boolean aarWithRadd;

    public String toLog() {
        return String.format(
                "recIndex=%d aarKey=%s numberOfPages=%s aarWithRadd=%s",
                recIndex,
                aarKey,
                numberOfPages,
                aarWithRadd
        );
    }
}
