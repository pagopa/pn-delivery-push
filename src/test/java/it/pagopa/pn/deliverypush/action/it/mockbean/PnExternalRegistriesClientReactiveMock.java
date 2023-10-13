package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PnExternalRegistriesClientReactiveMock implements PnExternalRegistriesClientReactive {
    public static final String TO_FAIL = "FAIL";
    public static final String TO_RETRY = "RETRY";
    private ConcurrentMap<String, Integer> notificationFees;
    public PnExternalRegistriesClientReactiveMock() {
        clear();
    }
    
    public void clear() {
        this.notificationFees = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest) {
        UpdateNotificationCostResponse response = new UpdateNotificationCostResponse();
        response.setIun(updateNotificationCostRequest.getIun());
        
        List<UpdateNotificationCostResult> updateResults =  new ArrayList<>();
        
        updateNotificationCostRequest.getPaymentsInfoForRecipients().forEach( paymentsInfoForRecipient -> {
            String key = getKey(paymentsInfoForRecipient.getCreditorTaxId(), paymentsInfoForRecipient.getNoticeCode());
            notificationFees.put(key,updateNotificationCostRequest.getNotificationStepCost());

            UpdateNotificationCostResult result = new UpdateNotificationCostResult();
            result.setCreditorTaxId(paymentsInfoForRecipient.getCreditorTaxId());
            result.setNoticeCode(paymentsInfoForRecipient.getNoticeCode());
            
            if(paymentsInfoForRecipient.getCreditorTaxId().contains(TO_FAIL)){
                result.setResult(UpdateNotificationCostResult.ResultEnum.KO);
            }else if(paymentsInfoForRecipient.getCreditorTaxId().contains(TO_RETRY)){
                result.setResult(UpdateNotificationCostResult.ResultEnum.RETRY);
            }else {
                result.setResult(UpdateNotificationCostResult.ResultEnum.OK);
            }
            
            updateResults.add(result);
        });

        response.setUpdateResults(updateResults);
        
        return Mono.just(response);
    }

    public Integer getNotificationCostFromIuv(String creditorTaxId, String noticeCode) {
        String key = getKey(creditorTaxId, noticeCode);
        return notificationFees.get(key);
    }

    @NotNull
    private static String getKey(String creditorTaxId, String noticeCode) {
        return creditorTaxId +"_"+ noticeCode;
    }
}
