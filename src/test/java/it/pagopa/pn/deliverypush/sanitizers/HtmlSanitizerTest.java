package it.pagopa.pn.deliverypush.sanitizers;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.*;
import static org.assertj.core.api.Assertions.assertThat;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import org.junit.jupiter.api.Test;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

class HtmlSanitizerTest {



    @Test
    void sanitizeStringWithoutHTMLElementTest() {
        String actualHTML = "Stringa che non contiene elementi HTML";
        String sanitized = HtmlSanitizerFactory.getDefault().sanitize(actualHTML);

        assertThat(sanitized).isEqualTo(actualHTML);

    }

    @Test
    void sanitizeStringWithJustImgHTMLElementTest() {
        String actualHTML = "Stringa che contiene una immagine <img src='https://www.prova.it'>";
        String sanitized = HtmlSanitizerFactory.getDefault().sanitize(actualHTML);
        System.out.println(sanitized);
        assertThat(sanitized).contains("<img");

    }

    @Test
    void sanitizeStringWithImgAndOtherHTMLElementsTest() {
        String actualHTML = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        String sanitized = HtmlSanitizerFactory.getDefault().sanitize(actualHTML);
        System.out.println(sanitized);
        assertThat(sanitized).contains("<img");
        assertThat(sanitized).doesNotContain("<h1>");
    }

    //test di non regressione; se non c'è alcun elemento HTML, mi aspetto di ricevere lo stesso identico model
    @Test
    void sanitizeNotificationIntWithNoHTMlElement() {
        Object templateModel = getTemplateModelForRequestAccepted(null);
        Object sanitized = HtmlSanitizerFactory.makeSanitizer(DocumentComposition.TemplateType.REQUEST_ACCEPTED).sanitize(templateModel);
        assertThat(sanitized).isEqualTo(templateModel);
    }

    @Test
    void sanitizeNotificationIntWithImgHTMlElement() {
        String customDenomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Object templateModel = getTemplateModelForRequestAccepted(customDenomination);

        Object sanitizedTemplateModel = HtmlSanitizerFactory.makeSanitizer(DocumentComposition.TemplateType.REQUEST_ACCEPTED).sanitize(templateModel);
        assertThat(sanitizedTemplateModel).isNotEqualTo(templateModel);
        assertThat(sanitizedTemplateModel).isInstanceOf(Map.class);

        Map<String, Object> templateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationInt notificationInt = (NotificationInt) templateModelMap.get(FIELD_NOTIFICATION);

        assertThat(notificationInt.getRecipients().get(0).getDenomination()).contains("<img").doesNotContain("<h1>");
    }

    private Map<String, Object> getTemplateModelForRequestAccepted(String customDenomination) {
        NotificationInt notification = buildNotification(customDenomination);
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate( notification.getSentAt() ) );
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate( notification.getSentAt(), true ) );
        templateModel.put(FIELD_NOTIFICATION, notification.toBuilder()
                .sender( notification.getSender().toBuilder()
                        .paDenomination( notification.getSender().getPaDenomination() )
                        .paTaxId( notification.getSender().getPaTaxId())
                        .build()
                )
                .build()
        );
        templateModel.put(FIELD_DIGESTS, Collections.emptyList() );
        templateModel.put(FIELD_ADDRESS_WRITER, new PhysicalAddressWriter());
        return templateModel;
    }

    private NotificationInt buildNotification(String denomination) {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now())
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref( NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(buildRecipients(denomination))
                .build();
    }

    private List<NotificationRecipientInt> buildRecipients(String denomination) {
        String defaultDenomination = StringUtils.hasText(denomination) ? denomination : "Galileo Bruno";
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();

        return Collections.singletonList( rec1 );
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }
}
