package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationAarForPec;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClientPec;

public class TemplatesClientMockPec implements TemplatesClientPec {
    private static final String RESULT_STRING = "Templates As String Result";

    @Override
    public String parametrizedNotificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec) {
        return RESULT_STRING;
    }
}
