package it.pagopa.pn.deliverypush.dto.cost;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class UpdateNotificationCostResponseInt {
    private String iun;
    private List<UpdateNotificationCostResultInt> updateResults;
}
