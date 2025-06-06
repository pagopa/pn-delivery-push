package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class DocumentCreationResponseEventHandler extends AbstractActionEventHandler {
    private final DocumentCreationResponseHandler documentCreationResponseHandler;

    public DocumentCreationResponseEventHandler(TimelineUtils timelineUtils, DocumentCreationResponseHandler documentCreationResponseHandler) {
        super(timelineUtils);
        this.documentCreationResponseHandler = documentCreationResponseHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.DOCUMENT_CREATION_RESPONSE;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.DOCUMENT_CREATION_RESPONSE.name();

            try {
                log.debug("Handle action of type DOCUMENT_CREATION_RESPONSE, with payload {}", action);

                DocumentCreationResponseActionDetails details = (DocumentCreationResponseActionDetails) action.getDetails();
                MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, details.getKey());

                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                log.logStartingProcess(processName);

                DocumentCreationResponseActionDetails documentCreationResponseActionDetails = (DocumentCreationResponseActionDetails) action.getDetails();
                if(DocumentCreationTypeInt.NOTIFICATION_CANCELLED.equals(documentCreationResponseActionDetails.getDocumentCreationType())){
                    //Solo se la si tratta della risposta alla generazione del documento di annullamento notifica viene bypassato il check di notifica annullata
                    documentCreationResponseHandler.handleResponseReceived(action.getIun(), action.getRecipientIndex(), documentCreationResponseActionDetails );
                }else {
                    checkNotificationCancelledAndExecute(
                            action,
                            a -> documentCreationResponseHandler.handleResponseReceived(a.getIun(), a.getRecipientIndex(), (DocumentCreationResponseActionDetails) a.getDetails() )
                    );
                }

                log.logEndingProcess(processName);

                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                HandleEventUtils.handleException(headers, ex);
                throw ex;
            }
    }
}
