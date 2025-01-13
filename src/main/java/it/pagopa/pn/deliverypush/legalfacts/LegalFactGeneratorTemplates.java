package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import it.pagopa.pn.deliverypush.utils.QrCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;
import static it.pagopa.pn.deliverypush.service.mapper.TemplatesEngineMapper.*;

@Slf4j
@AllArgsConstructor
public class LegalFactGeneratorTemplates implements LegalFactGenerator {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplatesClient templatesClient;

    /**
     * Generates the legal fact for a received notification.
     *
     * @param notification the {@link NotificationInt} object containing notification details,
     *                     including sender, recipients, and metadata.
     * @return a byte[] representing the generated pdf notification received legal fact.
     * @throws IllegalArgumentException if the notification is null or contains incomplete data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure the {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationReceivedLegalFact} and return the expected byte array.
     */
    @Override
    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) {
        log.info("retrieve NotificationReceivedLegalFact template for iun {}", notification.getIun());
        NotificationReceivedLegalFact legalFact = notificationReceivedLegalFact(notification, physicalAddressWriter, instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationReceivedLegalFact(language, legalFact);
    }


    /**
     * Generates the legal fact for the viewing of a notification.
     *
     * @param iun           the unique identifier of the notification (IUN).
     * @param recipient     the recipient of the notification, represented by a
     *                      {@link NotificationRecipientInt} object containing information such
     *                      as name (denomination) and tax ID.
     * @param delegateInfo  the delegate's information (if present), represented by a
     *                      {@link DelegateInfoInt} object containing name and tax ID.
     * @param timeStamp     the timestamp of when the notification was viewed, as an {@link Instant} object.
     * @param notification  the {@link NotificationInt} object representing the full notification,
     *                      from which additional information such as additional languages is extracted.
     * @return a byte array representing the pdf legal fact of the notification viewing.
     * @throws IllegalArgumentException if any required parameter is null or contains incomplete data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationViewedLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) {
        log.info("retrieve NotificationViewedLegalFact template for iun {}", iun);
        NotificationViewedLegalFact notificationViewedLegalFact = notificationViewedLegalFact(iun, recipient,
                delegateInfo, timeStamp, instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationViewedLegalFact(language, notificationViewedLegalFact);
    }

    /**
     * Generates the legal fact for the PEC delivery workflow.
     *
     * @param feedbackFromExtChannelList the list of {@link SendDigitalFeedbackDetailsInt} objects
     *                                   representing feedback from external digital channels, including
     *                                   delivery status and notification dates.
     * @param notification               the {@link NotificationInt} object containing details of the
     *                                   notification, such as its unique identifier (IUN).
     * @param recipient                  the recipient of the notification, represented by a
     *                                   {@link NotificationRecipientInt} object, containing information
     *                                   such as name (denomination) and tax ID.
     * @param status                     the {@link EndWorkflowStatus} representing the final status
     *                                   of the PEC delivery workflow (e.g., completed, failed).
     * @param completionWorkflowDate     the {@link Instant} representing the timestamp when the
     *                                   PEC delivery workflow was completed.
     * @return a byte array representing the pdf PEC delivery workflow legal fact.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is correctly configured to handle the generated
     * {@link PecDeliveryWorkflowLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) {
        log.info("retrieve PecDeliveryWorkflowLegalFact template for iun {}", notification.getIun());
        PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact = pecDeliveryWorkflowLegalFact(feedbackFromExtChannelList,
                notification, recipient,status, completionWorkflowDate, instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.pecDeliveryWorkflowLegalFact(language, pecDeliveryWorkflowLegalFact);
    }

    /**
     * Generates the legal fact for the failure of the analog delivery workflow.
     *
     * @param notification       the {@link NotificationInt} object containing the details of the notification,
     *                           such as its unique identifier (IUN).
     * @param recipient          the recipient of the notification, represented by a
     *                           {@link NotificationRecipientInt} object, which includes information such
     *                           as name (denomination) and tax ID.
     * @param status             the {@link EndWorkflowStatus} representing the final status of the analog
     *                           delivery workflow (e.g., failed).
     * @param failureWorkflowDate the {@link Instant} representing the timestamp when the analog delivery
     *                            workflow failure occurred.
     * @return a byte array representing the pdf legal fact for the analog delivery workflow failure.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link AnalogDeliveryWorkflowFailureLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) {
        log.info("retrieve AnalogDeliveryFailureWorkflowLegalFact template for iun {}", notification.getIun());
        AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact = analogDeliveryWorkflowFailureLegalFact(notification,
                recipient, failureWorkflowDate, instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.analogDeliveryWorkflowFailureLegalFact(language, analogDeliveryWorkflowFailureLegalFact);
    }

    /**
     * Generates the legal fact for a cancelled notification.
     *
     * @param notification                      the {@link NotificationInt} object containing details
     *                                          about the notification, including its unique identifier (IUN),
     *                                          sender information, and recipients.
     * @param notificationCancellationRequestDate the {@link Instant} representing the timestamp when
     *                                            the notification cancellation request was made.
     * @return a byte array representing the legal fact for the cancelled notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationCancelledLegalFact} object and return the expected pdf byte array.
     */
    @Override
    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {
        log.info("retrieve NotificationCancelledLegalFact template for iun {}", notification.getIun());
        NotificationCancelledLegalFact cancelledLegalFact = cancelledLegalFact(notification, notificationCancellationRequestDate, instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationCancelledLegalFact(language, cancelledLegalFact);
    }

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the subject line for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public String generateNotificationAARSubject(NotificationInt notification) {
        log.info("retrieve NotificationAARSubject template for iun {}", notification.getIun());
        NotificationAarForSubject notificationAARSubject = notificationAARSubject(notification);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSubject(language, notificationAARSubject);
    }

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details
     *                     about the notification, including its unique identifier (IUN)
     *                     and sender information.
     * @return a {@link AARInfo} representing the AAR info for the notification.
     *
     * @throws IllegalArgumentException if the notification is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(notification.getSentAt());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        String accessUrl = this.getAccessUrl(recipient);
        String accessUrlLabel = this.getAccessUrlLabel(recipient);
        String perfezionamentoLink = this.getPerfezionamentoLink();
        String perfezionamentoLinkLabel = this.getPerfezionamentoLinkLabel();
        String accessLink = this.getAccessLink();
        if (pnSendMode != null) {
            final AarTemplateChooseStrategy aarTemplateTypeChooseStrategy = pnSendMode.getAarTemplateTypeChooseStrategy();
            final AarTemplateType aarTemplateType = aarTemplateTypeChooseStrategy.choose(recipient.getPhysicalAddress());
            log.debug("aarTemplateType generated is ={} - iun={}", aarTemplateType, notification.getIun());
            byte[] bytesArrayGeneratedAar = new byte[0];
            switch (aarTemplateType) {
                case AAR_NOTIFICATION -> {
                    log.info("retrieve NotificationAAR template for iun {}", notification.getIun());
                    NotificationAar notificationAAR =  notificationAAR(notification, recipient, qrCodeQuickAccessUrlAarDetail,
                            accessUrl, accessUrlLabel, perfezionamentoLink, perfezionamentoLinkLabel);
                    bytesArrayGeneratedAar = templatesClient.notificationAar(language, notificationAAR);
                }
                case AAR_NOTIFICATION_RADD_ALT -> {
                    log.info("retrieve NotificationAARRADDalt template for iun {}", notification.getIun());
                    NotificationAarRaddAlt notificationAARRADDalt = notificationAARRADDalt(notification, recipient, qrCodeQuickAccessUrlAarDetail, accessUrl,
                            accessUrlLabel, accessLink, this.getAccessLinkLabel(), perfezionamentoLink, perfezionamentoLinkLabel);
                    bytesArrayGeneratedAar = templatesClient.notificationAarRaddAlt(language, notificationAARRADDalt);
                }
                case AAR_NOTIFICATION_RADD -> throw new PnInternalException("NotificationAAR_RADD not implemented", ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE);
            }
            return AARInfo.builder()
                    .bytesArrayGeneratedAar(bytesArrayGeneratedAar)
                    .templateType(aarTemplateType)
                    .build();
        } else {
            String msg = String.format("There isn't correct AAR configuration for date=%s - iun=%s", notification.getSentAt(), notification.getIun());
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND);
        }
    }

    /**
     * Generates the AAR body for a notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN).
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including relevant details such as contact information.
     * @param quickAccessToken  a {@link String} representing the token used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the body of the AAR email for the notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForEmail} object and return the expected email body string.
     */
    @Override
    public String generateNotificationAARBody(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        log.info("retrieve NotificationAARBody template for iun {}", notification.getIun());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        NotificationAarForEmail notificationAAR = notificationAarForEmail(this.getPerfezionamentoLink(),
                this.getPerfezionamentoLink(), qrCodeQuickAccessUrlAarDetail, this.getAccessUrl(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForEmail(language, notificationAAR);
    }

    /**
     * Generates the AAR body for a PEC notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN) and sender information.
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including their tax ID and other relevant details.
     * @param quickAccessToken  a {@link String} representing the token used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the PEC email body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForPec} object and return the expected PEC email body string.
     */
    @Override
    public String generateNotificationAARPECBody(NotificationInt notification,
                                                 NotificationRecipientInt recipient,
                                                 String quickAccessToken) {
        log.info("retrieve NotificationAARPECBody template for iun {}", notification.getIun());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        NotificationAarForPec notificationAAR = notificationAarForPec(notification, recipient, qrCodeQuickAccessUrlAarDetail,
                this.getPerfezionamentoLink(), this.getFAQSendURL(), this.getAccessUrl(recipient), this.getRecipientTypeForHTMLTemplate(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForPec(language, notificationAAR);
    }

    /**
     * Generates the AAR for an SMS notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the SMS body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSms} object and return the expected SMS body string.
     */
    @Override
    public String generateNotificationAARForSMS(NotificationInt notification) {
        log.info("retrieve NotificationAARForSMS template for iun {}", notification.getIun());
        NotificationAarForSms notificationAARForSMS = notificationAarForSms(notification);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSms(language, notificationAARForSMS);
    }

    /**
     * Retrieves the label for the access URL associated with a notification recipient.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the access URL.
     * @return a {@link String} representing the label of the access URL, typically the host without the "www." prefix.
     *         If the URL is invalid, returns the full access URL.
     */
    private String getAccessUrlLabel(NotificationRecipientInt recipient) {
        try {
            String host = new URL(getAccessUrl(recipient)).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return getAccessUrl(recipient);
        }
    }

    /**
     * Generates a Base64-encoded QR code image for quick access, using the provided recipient and access token.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to generate the quick access URL.
     * @param quickAccessToken the token used to generate the quick access URL, ensuring secure access to the resource.
     * @return a {@link String} representing the Base64-encoded QR code image in a data URI format.
     */
    private String getQrCodeQuickAccessUrlAarDetail(NotificationRecipientInt recipient, String quickAccessToken) {
        String url = getQuickAccessLink(recipient, quickAccessToken);
        // Definire altezza e larghezza del qrcode
        return "data:image/png;base64, " .concat(Base64Utils.encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180,
                pnDeliveryPushConfigs.getErrorCorrectionLevelQrCode())));
    }

    /**
     * Generates a quick access link URL for a given recipient, appending the quick access token to the base URL.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the base access URL.
     * @param quickAccessToken the token used to generate the quick access link, typically used for secure access.
     * @return a {@link String} representing the full quick access URL, including the token as a query parameter.
     */
    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccessToken) {
        String templateUrl = getAccessUrl(recipient) + pnDeliveryPushConfigs.getWebapp().getQuickAccessUrlAarDetailSuffix();
        log.debug("getQrCodeQuickAccessUrlAarDetail templateUrl {} quickAccessLink {}", templateUrl, quickAccessToken);
        return templateUrl + '=' + quickAccessToken;
    }

    /**
     * Generates a link to the "perfezionamento" page of the web application.
     *
     * @return a {@link String} representing the complete URL for the "perfezionamento" page.
     */
    private String getPerfezionamentoLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + "perfezionamento";
    }

    /**
     * Generates the label for the "perfezionamento" link by appending the "perfezionamento" path
     * to the base access link label.
     *
     * @return a {@link String} representing the complete label for the "perfezionamento" link.
     */
    private String getPerfezionamentoLinkLabel() {
        return this.getAccessLinkLabel() + "/perfezionamento";
    }

    private String getAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl();
    }

    /**
     * Retrieves the host name from the base landing URL configured in the application settings,
     * and removes the "www." prefix if it exists.
     *
     * @return a {@link String} representing the host name from the landing URL, with "www." removed if present.
     */
    private String getAccessLinkLabel() {
        try {
            String host = new URL(pnDeliveryPushConfigs.getWebapp().getLandingUrl()).getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return pnDeliveryPushConfigs.getWebapp().getLandingUrl();
        }
    }

    private String getFAQAccessLink() {
        return pnDeliveryPushConfigs.getWebapp().getLandingUrl() + pnDeliveryPushConfigs.getWebapp().getFaqUrlTemplateSuffix();
    }

    private String getFAQSendURL() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushConfigs.getWebapp().getFaqSendHash();
    }


    /**
     * Determines the recipient type for an HTML template based on the recipient's type.
     *
     * @param recipientInt the recipient object containing the recipient's type.
     * @return a {@link String} representing the recipient type for use in an HTML template,
     * either "giuridica" or "fisica".
     */
    private String getRecipientTypeForHTMLTemplate(NotificationRecipientInt recipientInt) {
        return recipientInt.getRecipientType() == RecipientTypeInt.PG ? "giuridica" : "fisica";
    }

    /**
     * Returns the appropriate access URL for a recipient based on their type.
     *
     * @param recipient the recipient object containing information about the recipient's type.
     * @return a {@link String} representing the access URL based on the recipient's type.
     */
    private String getAccessUrl(NotificationRecipientInt recipient) {
        return RecipientTypeInt.PF == recipient.getRecipientType()
                ? pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplatePhysical()
                : pnDeliveryPushConfigs.getWebapp().getDirectAccessUrlTemplateLegal();
    }

    /**
     * Determines the language to be used for the notification based on the provided list of additional languages.
     *
     * @param additionalLanguages a {@link List} of {@link String} representing the additional languages to be considered.
     *                            If the list is empty or null, the default language (Italian) is returned.
     * @return a {@link LanguageEnum} representing the selected language. It returns {@link LanguageEnum#IT}
     *         if no additional languages are available or enabled, otherwise the first language from the list.
     * @throws IllegalArgumentException if the provided list contains invalid language values.
     */
    private LanguageEnum getLanguage(List<String> additionalLanguages) {
        return (!pnDeliveryPushConfigs.isAdditionalLangsEnabled() || CollectionUtils.isEmpty(additionalLanguages))
                ? LanguageEnum.IT : LanguageEnum.fromValue(additionalLanguages.get(0));
    }

}

