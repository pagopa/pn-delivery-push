package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationAarForPec;

public interface TemplatesClientPec {

    String parametrizedNotificationAarForPec(LanguageEnum language, NotificationAarForPec notificationAarForPec);
}
