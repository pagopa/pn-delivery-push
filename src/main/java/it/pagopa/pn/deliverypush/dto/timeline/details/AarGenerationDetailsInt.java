package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AarGenerationDetailsInt implements RecipientRelatedTimelineElementDetails{
  private Integer recIndex;
  private String generatedAarUrl;
  private Integer numberOfPages;
}

