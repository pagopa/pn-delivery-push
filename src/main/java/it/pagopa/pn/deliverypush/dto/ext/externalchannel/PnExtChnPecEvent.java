package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import lombok.*;

import javax.validation.Valid;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PnExtChnPecEvent implements GenericEvent<StandardEventHeader, PnExtChnPecEventPayload> {

    @Valid
    private StandardEventHeader header;

    @Valid
    private PnExtChnPecEventPayload payload;

}
