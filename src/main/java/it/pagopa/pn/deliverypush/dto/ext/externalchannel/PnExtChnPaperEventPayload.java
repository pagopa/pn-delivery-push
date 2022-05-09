/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelType;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *
 * @author GIANGR40
 */
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PnExtChnPaperEventPayload {

    @Schema( description = "Codice opaco utilizzato dal software client per correlare la risposta alla richiesta" )
    private String requestCorrelationId;

    @Schema( description = "Identificativo univoco della richiesta" )
    @NotEmpty
    private String iun;
    
    @Schema( description = "Indirizzo fisico destinatario" )
    @NotNull
    private PhysicalAddress destinationAddress;
    
    @Schema( description = "Tipologia comunicazione" )
    @NotNull
    private CommunicationType communicationType;
    
    @Schema( description = "Livello Servizio" )
    @NotNull
    private ServiceLevelType serviceLevel;
    
    @Schema( description = "Denominazione Mittente" )
    @NotEmpty
    private String senderDenomination;
    
    @Schema( description = "Denominazione Destinatario" )
    @NotEmpty
    private String recipientDenomination;

    @Schema( description = "E' richiesta investigazione indirizzo fisico alternativo" )
    private boolean investigation;

    @Schema( description = "Link di accesso rapido" )
    @NotEmpty
    private String accessUrl;
}

