package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.NotificationViewedLegalFact;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.service.mapper.TemplatesEngineMapper.notificationViewedLegalFact;

@Slf4j
@AllArgsConstructor
@Component
public class LegalFactGeneratorTemplates implements LegalFactGenerator {

    private final CustomInstantWriter instantWriter;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final TemplatesClient templatesClient;

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
        NotificationViewedLegalFact notificationViewedLegalFact =
                notificationViewedLegalFact(
                        iun,
                        recipient,
                        delegateInfo,
                        timeStamp,
                        instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationViewedLegalFact(language, notificationViewedLegalFact);
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
