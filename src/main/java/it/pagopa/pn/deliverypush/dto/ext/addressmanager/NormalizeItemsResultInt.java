package it.pagopa.pn.deliverypush.dto.ext.addressmanager;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class NormalizeItemsResultInt {
    private String correlationId;
    private List<NormalizeResultInt> resultItems;

}
