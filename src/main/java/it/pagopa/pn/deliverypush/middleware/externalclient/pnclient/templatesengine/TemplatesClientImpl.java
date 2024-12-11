package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@CustomLog
public class TemplatesClientImpl implements TemplatesClient {

    private final TemplateApi templateEngineClient;

    @Override
    public byte[] notificationReceivedLegalFact(LanguageEnum xLanguage, NotificationReceivedLegalFact legalFact) {
        return templateEngineClient.notificationReceivedLegalFact(xLanguage, legalFact);
    }

    @Override
    public byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact) {
        return templateEngineClient.notificationViewedLegalFact(xLanguage, notificationViewedLegalFact);
    }

    @Override
    public byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact) {
        return templateEngineClient.pecDeliveryWorkflowLegalFact(xLanguage, pecDeliveryWorkflowLegalFact);
    }

    @Override
    public byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact) {
        return templateEngineClient.analogDeliveryWorkflowFailureLegalFact(xLanguage, analogDeliveryWorkflowFailureLegalFact);
    }

    @Override
    public byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact) {
        return templateEngineClient.notificationCancelledLegalFact(xLanguage, notificationCancelledLegalFact);
    }

    @Override
    public String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject) {
        return templateEngineClient.notificationAarForSubject(xLanguage, notificationAarForSubject);
    }

    @Override
    public byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar) {
        return templateEngineClient.notificationAar(xLanguage, notificationAar);
    }

    @Override
    public String notificationAarForSms(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms) {
        return templateEngineClient.notificationAarForSms(xLanguage, notificationAarForSms);
    }

    @Override
    public String notificationAarForEmail(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail) {
        return templateEngineClient.notificationAarForEmail(xLanguage, notificationAarForEmail);
    }

    @Override
    public String notificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec) {
        return templateEngineClient.notificationAarForPec(xLanguage, notificationAarForPec);
    }

    @Override
    public byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt) {
        return templateEngineClient.notificationAarRaddAlt(xLanguage, notificationAarRaddAlt);
    }

}
