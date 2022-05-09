package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.Valid;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaymentInfo {

    @Schema( description = "Identificativo Univoco del Versamento" )
    @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
    private String iuv;

    @Schema(  description = "Politica di addebitamento dei costi di notifica" )
    @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
    private NotificationPaymentInfoFeePolicies notificationFeePolicy;

    @Schema( description = "Moduli di pagamento F24" )
    @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
    @Valid
    private F24 f24 ;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder( toBuilder = true )
    @EqualsAndHashCode
    public static class F24 {

        @Schema( description = "Modulo F24 per spese di notifica forfettarie" )
        @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
        @Valid
        private NotificationAttachment flatRate;

        @Schema( description = "Modulo F24 per spese di notifica digitale" )
        @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
        @Valid
        private NotificationAttachment digital;

        @Schema( description = "Modulo F24 per spese di notifica analogica" )
        @JsonView(value = { it.pagopa.pn.api.dto.notification.NotificationJsonViews.New.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Sent.class, it.pagopa.pn.api.dto.notification.NotificationJsonViews.Received.class })
        @Valid
        private NotificationAttachment analog;
    }
}
