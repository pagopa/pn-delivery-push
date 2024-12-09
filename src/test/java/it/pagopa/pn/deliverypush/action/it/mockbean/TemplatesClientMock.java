package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;

import java.io.IOException;

public class TemplatesClientMock implements TemplatesClient {

    private static final String RESULT_STRING = "Templates As String Result";

    @Override
    public byte[] notificationReceivedLegalFact(LanguageEnum xLanguage, NotificationReceivedLegalFact notificationReceivedLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar) {
        return resultPdf();
    }

    @Override
    public byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt) {
        return resultPdf();
    }

    @Override
    public String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForSms(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForEmail(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec) {
        return RESULT_STRING;
    }

    private byte[] resultPdf() {
        try (var result = this.getClass().getResourceAsStream("/pdf/response.pdf")) {
            if (result == null) {
                throw new PnInternalException("resultPdf", "resultPdf no pdf found");
            }
            return result.readAllBytes();
        } catch (IOException ex) {
            throw new PnInternalException(ex.getMessage(), ex.getLocalizedMessage());
        }
    }
}
