package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Random;

@Slf4j
public class TestUtils {
    public static final String PN_NOTIFICATION_ATTACHMENT = "PN_NOTIFICATION_ATTACHMENT";
    public static final String TOO_BIG = "TOO_BIG";
    public static final String NOT_A_PDF = "NOT_A_PDF";


    private static int getTimes(boolean itWasGenerated) {
        return itWasGenerated ? 1 : 0;
    }


    public static NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }


    public static String getMethodName(final int depth) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[depth].getMethodName();
    }

    public static String getRandomIun(int level) {
        String callerMethod = getMethodName(level);
        return getIun(callerMethod);
    }

    @NotNull
    private static String getIun(String callerMethod) {
        Random rand = new Random();
        int upperbound = 10000;
        int int_random = rand.nextInt(upperbound);
        return "iun-" + callerMethod + "_" + int_random;
    }


    public static void initializeAllMockClient(SafeStorageClientMock safeStorageClientMock,
                                               PnDeliveryClientMock pnDeliveryClientMock,
                                               UserAttributesClientMock userAttributesClientMock,
                                               PaperNotificationFailedDaoMock paperNotificationFailedDaoMock,
                                               PnDataVaultClientMock pnDataVaultClientMock,
                                               PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock,
                                               DocumentCreationRequestDaoMock documentCreationRequestDaoMock
    ) {

        log.info("CLEARING MOCKS");

        ThreadPool.killThreads();

        safeStorageClientMock.clear();
        pnDeliveryClientMock.clear();
        userAttributesClientMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
        pnDataVaultClientReactiveMock.clear();
        documentCreationRequestDaoMock.clear();
        
        ConsoleAppenderCustom.initializeLog();
    }

}
