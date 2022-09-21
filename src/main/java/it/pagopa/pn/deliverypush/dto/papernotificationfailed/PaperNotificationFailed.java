package it.pagopa.pn.deliverypush.dto.papernotificationfailed;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PaperNotificationFailed {
    private String recipientId;
    private String iun;
}
