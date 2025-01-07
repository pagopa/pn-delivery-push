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
     * Generates a legal fact for a notification received event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationReceivedLegalFact The notification received legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationReceivedLegalFact(LanguageEnum xLanguage, NotificationReceivedLegalFact notificationReceivedLegalFact);

    /**
     * Generates a legal fact for a notification viewed event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationViewedLegalFact The notification viewed legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact);

    /**
     * Generates a legal fact for a PEC delivery workflow event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param pecDeliveryWorkflowLegalFact The PEC delivery workflow legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact);

    /**
     * Generates a legal fact for an analog delivery workflow failure event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param analogDeliveryWorkflowFailureLegalFact The analog delivery workflow failure legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact);

    /**
     * Generates a legal fact for a notification cancelled event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationCancelledLegalFact The notification cancelled legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact);

    /**
     * Generates an AAR (Accountability and Receipt) legal fact for a notification in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationAar The notification AAR to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar);

    /**
     * Generates an AAR (Accountability and Receipt) for RADD ALT for a notification in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationAarRaddAlt The notification AAR for RADD ALT to generate.
     * @return A byte array representing the generated legal fact.
     */
    byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt);

    /**
     * Generates the subject of an AAR (Accountability and Receipt) for a notification in the specified language.
     *
     * @param xLanguage The language for the subject.
     * @param notificationAarForSubject The notification AAR for subject to generate.
     * @return A string representing the generated subject.
     */
    String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject);

    /**
     * Generates an AAR (Accountability and Receipt) for SMS for a notification in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationAarForSms The notification AAR for SMS to generate.
     * @return A string representing the generated legal fact.
     */
    String notificationAarForSms(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms);

    /**
     * Generates an AAR (Accountability and Receipt) for email for a notification in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationAarForEmail The notification AAR for email to generate.
     * @return A string representing the generated legal fact.
     */
    String notificationAarForEmail(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail);

    /**
     * Generates an AAR (Accountability and Receipt) for PEC for a notification in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param notificationAarForPec The notification AAR for PEC to generate.
     * @return A string representing the generated legal fact.
     */
    String notificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec);
}
