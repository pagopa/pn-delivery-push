package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class AarGenerationDetailsInt extends GenericTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails{
  private int recIndex;
  private String generatedAarUrl;
  private Integer numberOfPages; //Nota il campo potrà essere eliminato in futuro dal momento che il numero di pagine viene calcolato da paperChannel

  public String toLog() {
    return String.format(
            "recIndex=%d",
            recIndex
    );
  }
  
}