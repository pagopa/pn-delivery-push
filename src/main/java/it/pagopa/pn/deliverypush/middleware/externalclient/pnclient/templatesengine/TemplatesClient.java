package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;

public interface TemplatesClient {

    byte[] notificationReceivedLegalFact(LanguageEnum xLanguage, NotificationReceivedLegalFact notificationReceivedLegalFact);

    byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact);

    byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact);

    byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact);

    byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact);

    byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar);

    byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt);

    String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject);

    String notificationAarForSms(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms);

    String notificationAarForEmail(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail);

    String notificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec);
}
