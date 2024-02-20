package it.pagopa.pn.deliverypush.action.documentcreationresponsehandler;

class DocumentCreationResponseHandlerTest {
//    @Mock
//    private ReceivedLegalFactCreationResponseHandler receivedLegalFactHandler;
//    @Mock
//    private AarCreationResponseHandler aarCreationResponseHandler;
//    @Mock
//    private NotificationViewLegalFactCreationResponseHandler notificationViewLegalFactCreationResponseHandler;
//    @Mock
//    private DigitalDeliveryCreationResponseHandler digitalDeliveryCreationResponseHandler;
//    @Mock
//    private AnalogFailureDeliveryCreationResponseHandler analogFailureDeliveryCreationResponseHandler;
//    @Mock
//    private TimelineUtils timelineUtils;
//
//    private DocumentCreationResponseHandler handler;
//
//    @BeforeEach
//    public void setup() {
//        handler = new DocumentCreationResponseHandler(receivedLegalFactHandler, aarCreationResponseHandler, notificationViewLegalFactCreationResponseHandler, digitalDeliveryCreationResponseHandler, analogFailureDeliveryCreationResponseHandler, timelineUtils);
//    }
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedSenderAck() {
//        //GIVEN
//        String iun = "testIun";
//        Integer recIndex = null;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
//                .build();
//
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(receivedLegalFactHandler).handleReceivedLegalFactCreationResponse(iun, details.getKey());
//    }
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedAAR() {
//        //GIVEN
//        String iun = "testIun";
//        int recIndex = 0;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.AAR)
//                .build();
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(aarCreationResponseHandler).handleAarCreationResponse(iun, recIndex, details);
//    }
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedDIGITAL_DELIVERY() {
//        //GIVEN
//        String iun = "testIun";
//        int recIndex = 0;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.DIGITAL_DELIVERY)
//                .build();
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(digitalDeliveryCreationResponseHandler).handleDigitalDeliveryCreationResponse(iun, recIndex, details);
//    }
//
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedANALOG_FAILURE_DELIVERY() {
//        //GIVEN
//        String iun = "testIun";
//        Integer recIndex = 0;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY)
//                .build();
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(analogFailureDeliveryCreationResponseHandler).handleAnalogFailureDeliveryCreationResponse(iun, recIndex, details);
//    }
//
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedRECIPIENT_ACCESS() {
//        //GIVEN
//        String iun = "testIun";
//        Integer recIndex = 0;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
//                .build();
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(notificationViewLegalFactCreationResponseHandler).handleLegalFactCreationResponse(iun, recIndex, details);
//    }
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseReceivedKo() {
//        //GIVEN
//        String iun = "testIun";
//        Integer recIndex = null;
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//                .key("legalFactId")
//                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
//                .build();
//
//
//        doThrow(new RuntimeException("ex")).when(receivedLegalFactHandler).handleReceivedLegalFactCreationResponse(Mockito.any(), Mockito.any());
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
//
//        //WHEN
//        Assertions.assertThrows(RuntimeException.class, () -> {
//            handler.handleResponseReceived(iun, recIndex, details);
//        });
//    }
//
//    @ExtendWith(SpringExtension.class)
//    @Test
//    void handleResponseCancelled() {
//        //GIVEN
//        String iun = "IUN-handleResponseCancelled";
//        int recIndex = 0;
//        String legalFactId = "legalFactId";
//        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
//            .key(legalFactId)
//            .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
//            .build();
//        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);
//
//        //WHEN
//        handler.handleResponseReceived(iun, recIndex, details);
//
//        //THEN
//        Mockito.verify(receivedLegalFactHandler, Mockito.never()).handleReceivedLegalFactCreationResponse(iun,legalFactId);
//        Mockito.verify(aarCreationResponseHandler, Mockito.never()).handleAarCreationResponse(iun, recIndex, details);
//        Mockito.verify(digitalDeliveryCreationResponseHandler, Mockito.never()).handleDigitalDeliveryCreationResponse(iun, recIndex, details);
//        Mockito.verify(analogFailureDeliveryCreationResponseHandler, Mockito.never()).handleAnalogFailureDeliveryCreationResponse(iun, recIndex, details);
//        Mockito.verify(notificationViewLegalFactCreationResponseHandler, Mockito.never()).handleLegalFactCreationResponse(iun, recIndex, details);
//    }
}