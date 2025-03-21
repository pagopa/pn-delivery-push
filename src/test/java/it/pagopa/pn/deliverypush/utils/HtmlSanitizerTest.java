package it.pagopa.pn.deliverypush.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorDocComposition;
import it.pagopa.pn.deliverypush.legalfacts.PhysicalAddressWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGeneratorDocComposition.*;
import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class HtmlSanitizerTest {

    private static final String FIELD_SIGNATURE = "signature";
    private static final String FIELD_TIME_REFERENCE = "timeReference";
    private static final String FIELD_PDF_FILE_NAME = "pdfFileName";

    @Autowired
    ObjectMapper objectMapper;

    HtmlSanitizer htmlSanitizer;

    @BeforeEach
    public void init() {
        htmlSanitizer = new HtmlSanitizer(objectMapper, HtmlSanitizer.SanitizeMode.DELETE_HTML);
    }


    @Test
    void sanitizeNumberTest() {
        int number = 10;
        int sanitized = (int) htmlSanitizer.sanitize(number);

        assertThat(sanitized).isEqualTo(number);

    }

    @Test
    void sanitizeInstantTest() {
        Instant now = Instant.now();
        Instant sanitized = (Instant) htmlSanitizer.sanitize(now);

        assertThat(sanitized).isEqualTo(now);

    }

    @Test
    void sanitizeNullTest() {
        Object sanitized = htmlSanitizer.sanitize(null);

        assertThat(sanitized).isNull();

    }

    @Test
    void sanitizeEmptyMapTest() {
        Map<Integer, String> emptyMap = Map.of();
        Object sanitized = htmlSanitizer.sanitize(emptyMap);

        assertThat(sanitized)
                .isInstanceOf(Map.class)
                .isEqualTo(emptyMap);
    }

    @Test
    void sanitizeStringWithSpecialCharacterTest() {
        String aString = "via dell'Aquila";
        Object sanitized = htmlSanitizer.sanitize(aString);

        assertThat(sanitized).isEqualTo(aString);

    }

    @Test
    void sanitizeLinkedListTest() {
        LinkedList<String> list = new LinkedList<>(List.of("Prova", "test", "l'aquila"));
        Object sanitized = htmlSanitizer.sanitize(list);

        assertThat(sanitized)
                .isInstanceOf(LinkedList.class)
                .isEqualTo(list);

    }

    @Test
    void sanitizeArrayListListTest() {
        ArrayList<String> list = new ArrayList<>(List.of("Prova", "test", "l'aquila"));
        Object sanitized = htmlSanitizer.sanitize(list);

        assertThat(sanitized)
                .isInstanceOf(ArrayList.class)
                .isEqualTo(list);

    }

    @Test
    void sanitizeHahSetTest() {
        HashSet<String> list = new HashSet<>(Set.of("Prova", "test", "l'aquila"));
        Object sanitized = htmlSanitizer.sanitize(list);

        assertThat(sanitized)
                .isInstanceOf(HashSet.class)
                .isEqualTo(list);

    }

    @Test
    void sanitizeTreeSetTest() {
        TreeSet<String> list = new TreeSet<>(Set.of("Prova", "test", "l'aquila"));
        Object sanitized = htmlSanitizer.sanitize(list);

        assertThat(sanitized)
                .isInstanceOf(TreeSet.class)
                .isEqualTo(list);

    }

    @Test
    void sanitizeLinkedSetTest() {
        NotificationRecipientInt notificationRecipientIntOne = buildRecipient("provaLinked");
        NotificationRecipientInt notificationRecipientIntTwo = buildRecipient("provaTwoLinked");
        LinkedHashSet<NotificationRecipientInt> list = new LinkedHashSet<>(Set.of(notificationRecipientIntOne, notificationRecipientIntTwo));
        Object sanitized = htmlSanitizer.sanitize(list);

        assertThat(sanitized)
                .isInstanceOf(LinkedHashSet.class)
                .isEqualTo(list);

    }

    @Test
    void sanitizeTreeMapTest() {
        Map<Integer, Integer> map = Map.of(1, 1, 2, 2);
        TreeMap<Integer, Integer> treeMapInput = new TreeMap<>(map);

        Object sanitized = htmlSanitizer.sanitize(treeMapInput);

        assertThat(sanitized)
                .isInstanceOf(TreeMap.class)
                .isEqualTo(treeMapInput);

    }

    @Test
    void sanitizeConcurrentHashMapTest() {
        Map<String, Integer> map = Map.of("key1", 1, "key2", 2);
        ConcurrentHashMap<String, Integer> concHashMapInput = new ConcurrentHashMap<>(map);

        Object sanitized = htmlSanitizer.sanitize(concHashMapInput);

        assertThat(sanitized)
                .isInstanceOf(ConcurrentHashMap.class)
                .isEqualTo(concHashMapInput);

    }

    @Test
    void sanitizeLinkedinHashMapTest() {
        NotificationRecipientInt notificationRecipientIntOne = buildRecipient("prova");
        NotificationRecipientInt notificationRecipientIntTwo = buildRecipient("provaTwo");
        Map<String, NotificationRecipientInt> map = Map.of("key1", notificationRecipientIntOne, "key2", notificationRecipientIntTwo);
        LinkedHashMap<String, NotificationRecipientInt> linkHashMapInput = new LinkedHashMap<>(map);

        Object sanitized = htmlSanitizer.sanitize(linkHashMapInput);

        assertThat(sanitized)
                .isInstanceOf(LinkedHashMap.class)
                .isEqualTo(linkHashMapInput);

    }


    @Test
    void sanitizeStringWithoutHTMLElementTest() {
        String actualHTML = "Stringa che non contiene elementi HTML";
        String sanitized = (String) htmlSanitizer.sanitize(actualHTML);

        assertThat(sanitized).isEqualTo(actualHTML);

    }


    @Test
    void sanitizeStringWithImgAndOtherHTMLElementsTest() {
        String actualHTML = "<html><h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img></html>";
        String sanitized = (String) htmlSanitizer.sanitize(actualHTML);
        System.out.println(sanitized);
        assertThat(sanitized).doesNotContain("<img", "<h1>", "<html>");
    }

    //test di non regressione; se non c'è alcun elemento HTML, mi aspetto di ricevere lo stesso identico model
    @Test
    void sanitizeRequestAcceptedTemplateWithNoHTMlElement() {
        Map<?, ?> templateModel = getTemplateModelForRequestAccepted(null);
        System.out.println("OLD MAP: " + templateModel);
        NotificationInt notificationInt = (NotificationInt) templateModel.get(FIELD_NOTIFICATION);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        System.out.println("NEW MAP: " + sanitizedTemplateModelMap);
        NotificationInt sanitizedNotificationInt = (NotificationInt) sanitizedTemplateModelMap.get(FIELD_NOTIFICATION);

        assertThat(sanitizedNotificationInt).isEqualTo(notificationInt);

        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_SEND_DATE, templateModel.get(FIELD_SEND_DATE))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME))
                .containsEntry(FIELD_DIGESTS, templateModel.get(FIELD_DIGESTS));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    @Test
    void sanitizeRequestAcceptedTemplateWithImgHTMlElement() {
        String customDenomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Map<?, ?> templateModel = getTemplateModelForRequestAccepted(customDenomination);
        NotificationInt notificationInt = (NotificationInt) templateModel.get(FIELD_NOTIFICATION);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationInt sanitizedNotificationInt = (NotificationInt) sanitizedTemplateModelMap.get(FIELD_NOTIFICATION);

        assertThat(sanitizedNotificationInt.getRecipients().get(0).getDenomination()).doesNotContain("<h1>", "<img");
        assertThat(sanitizedNotificationInt.getIun()).isEqualTo(notificationInt.getIun());
        assertThat(sanitizedNotificationInt.getSender()).isEqualTo(notificationInt.getSender());
        assertThat(sanitizedNotificationInt.getAmount()).isEqualTo(notificationInt.getAmount());
        assertThat(sanitizedNotificationInt.getPaProtocolNumber()).isEqualTo(notificationInt.getPaProtocolNumber());
        assertThat(sanitizedNotificationInt.getSubject()).isEqualTo(notificationInt.getSubject());
        assertThat(sanitizedNotificationInt.getSentAt()).isEqualTo(notificationInt.getSentAt());
        assertThat(sanitizedNotificationInt.getDocuments()).isEqualTo(notificationInt.getDocuments());

        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_SEND_DATE, templateModel.get(FIELD_SEND_DATE))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME))
                .containsEntry(FIELD_DIGESTS, templateModel.get(FIELD_DIGESTS));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    @Test
    void sanitizeNotificationViewedTemplateWithNoHTMlElement() {
        Map<?, ?> templateModel = getTemplateModelForNotificationViewed(null);
        NotificationRecipientInt notificationRecipientInt = (NotificationRecipientInt) templateModel.get(FIELD_RECIPIENT);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationRecipientInt sanitizedNotificationRecipientInt = (NotificationRecipientInt) sanitizedTemplateModelMap.get(FIELD_RECIPIENT);

        assertThat(sanitizedNotificationRecipientInt).isEqualTo(notificationRecipientInt);

        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_IUN, templateModel.get(FIELD_IUN))
                .containsEntry(FIELD_WHEN, templateModel.get(FIELD_WHEN))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    @Test
    void sanitizeNotificationViewedTemplateWithImgHTMlElement() {
        String customDenomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Map<?, ?> templateModel = getTemplateModelForNotificationViewed(customDenomination);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationRecipientInt notificationRecipientInt = (NotificationRecipientInt) sanitizedTemplateModelMap.get(FIELD_RECIPIENT);

        assertThat(notificationRecipientInt.getDenomination()).doesNotContain("<h1>", "<img");
        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_IUN, templateModel.get(FIELD_IUN))
                .containsEntry(FIELD_WHEN, templateModel.get(FIELD_WHEN))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    @Test
    void sanitizeDigitalNotificationWorkflowTemplateWithNoHTMlElement() {
        Object templateModel = getTemplateModelForDigitalNotificationWorkflow(null, RecipientTypeInt.PF);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat(sanitizedTemplateModel).isEqualTo(templateModel);
    }

    @Test
    void sanitizeDigitalNotificationWorkflowTemplateWithImgHTMlElement() {
        String customDenomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Map<?, ?> templateModel = getTemplateModelForDigitalNotificationWorkflow(customDenomination, RecipientTypeInt.PF);
        List<LegalFactGeneratorDocComposition.PecDeliveryInfo> deliveryInfosActual = (List<LegalFactGeneratorDocComposition.PecDeliveryInfo>) templateModel.get(FIELD_DELIVERIES);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        List<LegalFactGeneratorDocComposition.PecDeliveryInfo> sanitizedDeliveryInfos = (List<LegalFactGeneratorDocComposition.PecDeliveryInfo>) sanitizedTemplateModelMap.get(FIELD_DELIVERIES);

        assertThat(sanitizedDeliveryInfos).isNotNull().hasSize(1);
        assertThat(sanitizedDeliveryInfos.get(0).getDenomination()).doesNotContain("<h1>", "<img");
        assertThat(sanitizedDeliveryInfos.get(0).getAddress()).isEqualTo(deliveryInfosActual.get(0).getAddress());
        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_IUN, templateModel.get(FIELD_IUN))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME));
    }

    @Test
    void sanitizeFileComplianceTemplateWithNoHTMlElement() {
        Object templateModel = getTemplateModelForFileCompliance(null);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat(sanitizedTemplateModel).isEqualTo(templateModel);
    }

    @Test
    void sanitizeFileComplianceTemplateWithImgHTMlElement() {
        String customSignature = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Map<?, ?> templateModel = getTemplateModelForFileCompliance(customSignature);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;

        assertThat((String) sanitizedTemplateModelMap.get(FIELD_SIGNATURE)).doesNotContain("<h1>", "<img");
        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_TIME_REFERENCE, templateModel.get(FIELD_TIME_REFERENCE))
                .containsEntry(FIELD_PDF_FILE_NAME, templateModel.get(FIELD_PDF_FILE_NAME))
                .containsEntry(FIELD_SEND_DATE, templateModel.get(FIELD_SEND_DATE));
    }

    @Test
    void sanitizeAARNotificationTemplateWithNoHTMlElement() {
        Map<?, ?> templateModel = getTemplateModelForAARNotification(null);
        NotificationInt notificationInt = (NotificationInt) templateModel.get(FIELD_NOTIFICATION);
        NotificationRecipientInt notificationRecipientInt = (NotificationRecipientInt) templateModel.get(FIELD_RECIPIENT);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);

        assertThat(sanitizedTemplateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationInt sanitizedNotificationInt = (NotificationInt) sanitizedTemplateModelMap.get(FIELD_NOTIFICATION);
        NotificationRecipientInt sanitizedNotificationRecipientInt = (NotificationRecipientInt) sanitizedTemplateModelMap.get(FIELD_RECIPIENT);

        assertThat(sanitizedNotificationInt).isEqualTo(notificationInt);
        assertThat(sanitizedNotificationRecipientInt).isEqualTo(notificationRecipientInt);

        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_SEND_DATE, templateModel.get(FIELD_SEND_DATE))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    @Test
    void sanitizeAARNotificationTemplateWithImgHTMlElement() {
        String customDenomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        Map<?, ?> templateModel = getTemplateModelForAARNotification(customDenomination);
        NotificationInt notificationInt = (NotificationInt) templateModel.get(FIELD_NOTIFICATION);
        NotificationRecipientInt notificationRecipientInt = (NotificationRecipientInt) templateModel.get(FIELD_RECIPIENT);

        Object sanitizedTemplateModel = htmlSanitizer.sanitize(templateModel);
        assertThat((Map<?, ?>) sanitizedTemplateModel).isNotEqualTo(templateModel).isInstanceOf(Map.class);

        Map<String, Object> sanitizedTemplateModelMap = (Map<String, Object>) sanitizedTemplateModel;
        NotificationInt sanitizedNotificationInt = (NotificationInt) sanitizedTemplateModelMap.get(FIELD_NOTIFICATION);
        NotificationRecipientInt sanitizedNotificationRecipientInt = (NotificationRecipientInt) sanitizedTemplateModelMap.get(FIELD_RECIPIENT);


        assertThat(sanitizedNotificationInt.getRecipients().get(0).getDenomination()).doesNotContain("<h1>", "<img");
        assertThat(sanitizedNotificationInt.getIun()).isEqualTo(notificationInt.getIun());
        assertThat(sanitizedNotificationInt.getSender()).isEqualTo(notificationInt.getSender());
        assertThat(sanitizedNotificationInt.getAmount()).isEqualTo(notificationInt.getAmount());
        assertThat(sanitizedNotificationInt.getPaProtocolNumber()).isEqualTo(notificationInt.getPaProtocolNumber());
        assertThat(sanitizedNotificationInt.getSubject()).isEqualTo(notificationInt.getSubject());
        assertThat(sanitizedNotificationInt.getSentAt()).isEqualTo(notificationInt.getSentAt());
        assertThat(sanitizedNotificationInt.getDocuments()).isEqualTo(notificationInt.getDocuments());

        assertThat(sanitizedNotificationRecipientInt.getDenomination()).doesNotContain("<h1>", "<img");
        assertThat(sanitizedNotificationRecipientInt.getTaxId()).isEqualTo(notificationRecipientInt.getTaxId());

        assertThat(sanitizedTemplateModelMap)
                .containsEntry(FIELD_SEND_DATE, templateModel.get(FIELD_SEND_DATE))
                .containsEntry(FIELD_SEND_DATE_NO_TIME, templateModel.get(FIELD_SEND_DATE_NO_TIME));
        assertThat(sanitizedTemplateModelMap.get(FIELD_ADDRESS_WRITER)).isNotNull().isInstanceOf(PhysicalAddressWriter.class);
    }

    private Map<String, Object> getTemplateModelForRequestAccepted(String customDenomination) {
        NotificationInt notification = buildNotification(customDenomination);
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, instantWriter.instantToDate(notification.getSentAt()));
        templateModel.put(FIELD_SEND_DATE_NO_TIME, instantWriter.instantToDate(notification.getSentAt(), true));
        templateModel.put(FIELD_NOTIFICATION, notification.toBuilder()
                .sender(notification.getSender().toBuilder()
                        .paDenomination(notification.getSender().getPaDenomination())
                        .paTaxId(notification.getSender().getPaTaxId())
                        .build()
                )
                .build()
        );
        templateModel.put(FIELD_DIGESTS, Collections.emptyList());
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
                                        .ref(NotificationDocumentInt.Ref.builder()
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
                .recipients(Collections.singletonList(buildRecipient(denomination)))
                .build();
    }

    private NotificationRecipientInt buildRecipient(String denomination) {
        String defaultDenomination = StringUtils.hasText(denomination) ? denomination : "Galileo Bruno";
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(buildPhysicalAddressInt())
                .build();

        return rec1;
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return new PhysicalAddressInt(
                "Galileo Bruno",
                "Palazzo dell'Inquisizione",
                "corso Italia 666",
                "Piano Terra (piatta)",
                "00100",
                "Roma",
                null,
                "RM",
                "IT"
        );
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }

    private Map<String, Object> getTemplateModelForNotificationViewed(String denomination) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_IUN, UUID.randomUUID().toString());
        templateModel.put(FIELD_RECIPIENT, buildRecipient(denomination));
        templateModel.put(FIELD_WHEN, Instant.now().toString());
        templateModel.put(FIELD_ADDRESS_WRITER, new PhysicalAddressWriter());
        templateModel.put(FIELD_SEND_DATE_NO_TIME, Instant.now().toString());
        return templateModel;
    }

    private Map<String, Object> getTemplateModelForDigitalNotificationWorkflow(String denomination, RecipientTypeInt recipientType) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE_NO_TIME, Instant.now().toString());
        templateModel.put(FIELD_IUN, UUID.randomUUID().toString());
        templateModel.put(FIELD_DELIVERIES, Collections.singletonList(buildPecDeliveryInfo(denomination, recipientType)));
        return templateModel;
    }

    private LegalFactGeneratorDocComposition.PecDeliveryInfo buildPecDeliveryInfo(String denomination, RecipientTypeInt recipientType) {
        return new LegalFactGeneratorDocComposition.PecDeliveryInfo(
                denomination,
                UUID.randomUUID().toString(),
                recipientType,
                "digital address",
                "digital address source",
                "PEC",
                Instant.now(),
                Instant.now().toString(),
                true
        );
    }

    private Map<String, Object> getTemplateModelForFileCompliance(String signature) {
        Map<String, Object> templateModel = new HashMap<>();
        String defaultSignature = StringUtils.hasText(signature) ? signature : "Default Signature";
        templateModel.put(FIELD_SIGNATURE, defaultSignature);
        templateModel.put(FIELD_TIME_REFERENCE, Instant.now());
        templateModel.put(FIELD_PDF_FILE_NAME, "PDF Name");
        templateModel.put(FIELD_SEND_DATE, Instant.now().toString());
        return templateModel;
    }

    private Map<String, Object> getTemplateModelForAARNotification(String denomination) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(FIELD_SEND_DATE, Instant.now().toString());
        templateModel.put(FIELD_SEND_DATE_NO_TIME, Instant.now().toString());
        templateModel.put(FIELD_NOTIFICATION, buildNotification(denomination));
        templateModel.put(FIELD_RECIPIENT, buildRecipient(denomination));
        templateModel.put(FIELD_ADDRESS_WRITER, new PhysicalAddressWriter());
        return templateModel;
    }

}
