package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.PaymentsInfoForRecipient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NotificationCostResponseMapper {

    private NotificationCostResponseMapper() {
    }

    public static UpdateNotificationCostRequest internalToExternal(int notificationStepCost,
                                                  String iun,
                                                  List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients,
                                                  Instant eventTimestamp,
                                                  Instant eventStorageTimestamp,
                                                  UpdateCostPhaseInt updateCostPhase){
        UpdateNotificationCostRequest updateNotificationCostRequest = new UpdateNotificationCostRequest();
        updateNotificationCostRequest.setNotificationStepCost(notificationStepCost);
        updateNotificationCostRequest.setUpdateCostPhase(UpdateNotificationCostRequest.UpdateCostPhaseEnum.valueOf(updateCostPhase.getValue()));
        updateNotificationCostRequest.setIun(iun);
        updateNotificationCostRequest.setEventStorageTimestamp(eventStorageTimestamp);
        updateNotificationCostRequest.setEventTimestamp(eventTimestamp);
        updateNotificationCostRequest.setPaymentsInfoForRecipients(
                paymentsInfoForRecipients.stream().map(elem ->
                        new PaymentsInfoForRecipient()
                                .creditorTaxId(elem.getCreditorTaxId())
                                .noticeCode(elem.getNoticeCode())
                                .recIndex(elem.getRecIndex())
                ).toList()
        );
        return updateNotificationCostRequest;
    }

    public static UpdateNotificationCostResponseInt externalToInternal(UpdateNotificationCostResponse updateNotificationCostResponse) {

        List<UpdateNotificationCostResultInt> updateResults = new ArrayList<>();
        
        updateNotificationCostResponse.getUpdateResults().forEach(elem ->
                updateResults.add(UpdateNotificationCostResultInt.builder()
                        .paymentsInfoForRecipient(
                                PaymentsInfoForRecipientInt.builder()
                                        .creditorTaxId(elem.getCreditorTaxId())
                                        .noticeCode(elem.getNoticeCode())
                                        .recIndex(elem.getRecIndex())
                                        .build()
                        )
                        .result(UpdateNotificationCostResultInt.ResultEnum.valueOf(elem.getResult().getValue()))
                        .build())
        );

        return UpdateNotificationCostResponseInt.builder().updateResults(updateResults)
                .build();
    }
    
    public static NotificationCostResponseInt externalToInternal(NotificationCostResponse dtoExt) {
        return NotificationCostResponseInt.builder()
                .iun(dtoExt.getIun())
                .recipientIdx(dtoExt.getRecipientIdx())
                .build();
    }
}
