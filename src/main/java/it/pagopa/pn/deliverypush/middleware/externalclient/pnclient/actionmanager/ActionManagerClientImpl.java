package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTION_CONFLICT;


@RequiredArgsConstructor
@CustomLog
@Component
public class ActionManagerClientImpl extends CommonBaseClient implements ActionManagerClient {
    private final ActionApi actionManagerApi;

    @Override
    public void addOnlyActionIfAbsent(NewAction action) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, ADD_ONLY_ACTION_IF_ABSENT_PROCESS_NAME, action.getActionId());
        try {
            actionManagerApi.insertAction(action);
        } catch (PnHttpResponseException pnHttpResponseException) {
            if (pnHttpResponseException.getStatusCode() == HttpStatus.CONFLICT.value()
                    && pnHttpResponseException.getProblem().getErrors().get(0).getCode().equals(ERROR_CODE_DELIVERYPUSH_ACTION_CONFLICT)) {
                log.warn("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId={}", action.getActionId());
            }
            else {
                throw pnHttpResponseException;
            }
        }
    }
}
