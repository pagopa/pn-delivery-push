package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;

/**
 * Interface for generating legal facts related to notifications and workflows using a template engine.
 * <p>
 * This interface defines methods for generating various types of legal facts based on notification events,
 * including notifications that are received, viewed, cancelled, or related to specific workflows (e.g., PEC delivery, analog delivery failures).
 * Each method takes a {@link LanguageEnum} to specify the language of the generated legal fact and the appropriate legal fact object.
 * </p>
 * <p>
 * The methods in this interface cover a variety of legal fact generation scenarios, including but not limited to:
 * <ul>
 *     <li>Notification received legal fact</li>
 *     <li>Notification viewed legal fact</li>
 *     <li>Pec delivery workflow legal fact</li>
 *     <li>Analog delivery workflow failure legal fact</li>
 *     <li>Notification cancelled legal fact</li>
 *     <li>Notification AAR (Accountability and Receipt) for different formats (subject, SMS, email, PEC, RADD ALT)</li>
 * </ul>
 * </p>
 */
public interface TemplatesClient {

    /**
     * Generates a legal fact for a notification viewed event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationViewedLegalFact The notification viewed legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact);

}
