package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.api.TemplateApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
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
     * Generates a legal fact for a notification received event in the specified language.
     *
     * @param xLanguage The language for the legal fact.
     * @param legalFact The notification received legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] notificationReceivedLegalFact(LanguageEnum xLanguage, NotificationReceivedLegalFact legalFact) {
        return templateEngineClient.notificationReceivedLegalFact(xLanguage, legalFact);
    }

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

    /**
     * Generates a legal fact for a PEC delivery workflow event in the specified language.
     *
     * @param xLanguage                    The language for the legal fact.
     * @param pecDeliveryWorkflowLegalFact The PEC delivery workflow legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact) {
        return templateEngineClient.pecDeliveryWorkflowLegalFact(xLanguage, pecDeliveryWorkflowLegalFact);
    }

    /**
     * Generates a legal fact for an analog delivery workflow failure event in the specified language.
     *
     * @param xLanguage                              The language for the legal fact.
     * @param analogDeliveryWorkflowFailureLegalFact The analog delivery workflow failure legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact) {
        return templateEngineClient.analogDeliveryWorkflowFailureLegalFact(xLanguage, analogDeliveryWorkflowFailureLegalFact);
    }

    /**
     * Generates a legal fact for a notification cancelled event in the specified language.
     *
     * @param xLanguage                      The language for the legal fact.
     * @param notificationCancelledLegalFact The notification cancelled legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact) {
        return templateEngineClient.notificationCancelledLegalFact(xLanguage, notificationCancelledLegalFact);
    }

    /**
     * Generates the subject of an AAR for a notification in the specified language.
     *
     * @param xLanguage                 The language for the subject.
     * @param notificationAarForSubject The notification AAR for subject to generate.
     * @return A string representing the generated subject.
     */
    @Override
    public String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject) {
        return templateEngineClient.notificationAarForSubject(xLanguage, notificationAarForSubject);
    }

    /**
     * Generates an AAR legal fact for a notification in the specified language.
     *
     * @param xLanguage       The language for the legal fact.
     * @param notificationAar The notification AAR to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar) {
        return templateEngineClient.notificationAar(xLanguage, notificationAar);
    }

    /**
     * Generates an AAR for SMS for a notification in the specified language.
     *
     * @param xLanguage             The language for the legal fact.
     * @param notificationAarForSms The notification AAR for SMS to generate.
     * @return A string representing the generated legal fact.
     */
    @Override
    public String notificationAarForSms(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms) {
        return templateEngineClient.notificationAarForSms(xLanguage, notificationAarForSms);
    }

    /**
     * Generates an AAR for email for a notification in the specified language.
     *
     * @param xLanguage               The language for the legal fact.
     * @param notificationAarForEmail The notification AAR for email to generate.
     * @return A string representing the generated legal fact.
     */
    @Override
    public String notificationAarForEmail(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail) {
        return templateEngineClient.notificationAarForEmail(xLanguage, notificationAarForEmail);
    }

    /**
     * Generates an AAR for PEC for a notification in the specified language.
     *
     * @param xLanguage             The language for the legal fact.
     * @param notificationAarForPec The notification AAR for PEC to generate.
     * @return A string representing the generated legal fact.
     */
    @Override
    public String notificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec) {
        return templateEngineClient.notificationAarForPec(xLanguage, notificationAarForPec);
    }

    /**
     * Generates an AAR for RADD ALT for a notification in the specified language.
     *
     * @param xLanguage              The language for the legal fact.
     * @param notificationAarRaddAlt The notification AAR for RADD ALT to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt) {
        return templateEngineClient.notificationAarRaddAlt(xLanguage, notificationAarRaddAlt);
    }

    /**
     * Generates a PDF based on AnalogDeliveryWorkflowTimeoutLegalFact template.
     *
     * @param xLanguage The language for the legal fact.
     * @param analogDeliveryWorkflowTimeoutLegalFact The analog delivery workflow timeout legal fact to generate.
     * @return A byte array representing the generated legal fact.
     */
    @Override
    public byte[] analogDeliveryWorkflowTimeoutLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowTimeoutLegalFact analogDeliveryWorkflowTimeoutLegalFact) {
        return templateEngineClient.analogDeliveryWorkflowTimeoutLegalFact(xLanguage, analogDeliveryWorkflowTimeoutLegalFact);
    }

}
