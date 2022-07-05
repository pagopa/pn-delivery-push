package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotHandledDetailsInt implements RecipientRelatedTimelineElementDetails{
  public static final String PAPER_MESSAGE_NOT_HANDLED_CODE = "001";
  public static final String PAPER_MESSAGE_NOT_HANDLED_REASON = "Paper message not handled";

  private int recIndex;
  private String reasonCode;
  private String reason;
}

