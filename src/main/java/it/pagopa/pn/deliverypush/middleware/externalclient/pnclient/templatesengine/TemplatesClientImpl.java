package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationViewedLegalFact;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link TemplatesClient} interface for interacting with a template engine to generate various legal facts.
 * <p>
 * This class is responsible for delegating calls to the {@link TemplateApi} client for generating legal facts in byte array or string format.
 * </p>
 */
@Component
@RequiredArgsConstructor
@CustomLog
public class TemplatesClientImpl implements TemplatesClient {

    private final TemplateApi templateEngineClient;

    /**
     * Generates a legal fact for a notification viewed event in the specified language.
     *
     * @param xLanguage                   The language for the legal fact.
     * @param notificationViewedLegalFact The notification viewed legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact) {
        return templateEngineClient.notificationViewedLegalFact(xLanguage, notificationViewedLegalFact);
    }

}
