package it.pagopa.pn.deliverypush.external;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AddressBookEntry {

    @Schema(description = "Codice fiscale della persona fisica o giuridica")
    private String taxId;

    @Schema(description = "Indirizzo digitale di piattaforma")
    private DigitalAddress platformDigitalAddress;

    @Schema(description = "Indirizzi recapito digitale utilizzabili per la persona indicata dal TaxId")
    private List<DigitalAddress> courtesyAddresses;

    @Schema(description = "Indirizzo recapito analogico per la persona indicata dal TaxId")
    private PhysicalAddress residentialAddress;

}
