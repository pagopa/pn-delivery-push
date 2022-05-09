package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;


import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationRecipient {

    @Schema( description = "Tipologia di destinatario: Persona Fisica (PF) o Persona Giuridica (PG)")
    @JsonView(value = { NotificationJsonViews.New.class, NotificationJsonViews.Sent.class})
    @NotNull(groups = { NotificationJsonViews.New.class })
    private NotificationRecipientType recipientType;

    @Schema( description = "Codice Fiscale del destinatario")
    @JsonView(value = { NotificationJsonViews.New.class, NotificationJsonViews.Sent.class})
    @NotBlank(groups = { NotificationJsonViews.New.class })
    private String taxId;

    @Schema( description = "Nome e cognome / ragione sociale")
    @JsonView(value = { NotificationJsonViews.New.class, NotificationJsonViews.Sent.class})
    @NotBlank(groups = { NotificationJsonViews.New.class })
    private String denomination;

    @Schema( description = "indirizzo digitale del destinatario")
    @JsonView(value = { NotificationJsonViews.New.class, NotificationJsonViews.Sent.class})
    //@NotNull(groups = { NotificationJsonViews.New.class })
    @Valid
    private DigitalAddress digitalDomicile;

    @Schema( description = "indirizzo fisico del destinatario")
    @JsonView(value = { NotificationJsonViews.New.class, NotificationJsonViews.Sent.class})
    private PhysicalAddress physicalAddress;

    /**
     * token di accesso diretto
     */
    private String token;
}
