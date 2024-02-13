package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TimelineElementJsonConverterTest {
    private ObjectMapper objectMapper = new ObjectMapper();
    private TimelineElementJsonConverter converter = new TimelineElementJsonConverter(objectMapper);


    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.converter = new TimelineElementJsonConverter(this.objectMapper);
    }

    @Test
    void test_convertEntityToJson() {
        TimelineElementEntity entity = Mockito.mock(TimelineElementEntity.class);

        String expected = """
        {"timelineElementId":null,"iun":null,"statusInfo":null,"notificationSentAt":null,"paId":null,"legalFactIds":[],"details":null,"category":null,"timestamp":null}""";

        String json = converter.entityToJson(entity);
        assertNotNull(json);
        assertTrue(json.contains(expected));
    }

    @Test
    void test_convertJsonEntity() {
        String jsonResult = """
                {
                "timelineElementId": "1234",
                "iun": "1234",
                "statusInfo":null,
                "notificationSentAt":null,
                "paId":null,
                "legalFactIds":[],
                "details":null,
                "category":"SENDER_ACK_CREATION_REQUEST",
                "timestamp":null,
                "details": {
                                     "recIndex": 0,
                                     "physicalAddress": {
                                       "at": "at_53a36e52e575",
                                       "address": "address_dc5fa19533ce",
                                       "addressDetails": "addressDetails_42aceb7a6e5d",
                                       "zip": "zip_e41981b80b4a",
                                       "municipality": "municipality_e6790efc3379",
                                       "municipalityDetails": "municipalityDetails_ba0c8ee5842f",
                                       "province": "province_d1cc621a5369",
                                       "foreignState": "foreignState_06c962d4a276"
                                     },
                                     "digitalAddress": {
                                       "type": "PEC",
                                       "address": "address_49a83f383a06"
                                     },
                                     "digitalAddressSource": "PLATFORM",
                                     "isAvailable": false,
                                     "attemptDate": "2024-02-09T15:30:00Z",
                                     "deliveryMode": "DIGITAL",
                                     "contactPhase": "CHOOSE_DELIVERY",
                                     "sentAttemptMade": 0,
                                     "sendDate": "2024-02-09T15:30:00Z",
                                     "errors": [
                                       "errors_892097ce261e"
                                     ],
                                     "numberOfRecipients": 0,
                                     "lastAttemptDate": "2024-02-09T15:30:00Z",
                                     "retryNumber": 0,
                                     "downstreamId": {
                                       "systemId": "systemId_e6a0d0dbd3e6",
                                       "messageId": "messageId_b563a657ac39"
                                     },
                                     "responseStatus": "OK",
                                     "notificationDate": "2024-02-09T15:30:00Z",
                                     "serviceLevel": "AR_REGISTERED_LETTER",
                                     "investigation": false,
                                     "relatedRequestId": "relatedRequestId_9909b8bd4435",
                                     "newAddress": {
                                       "at": "at_8e9854fb7cf6",
                                       "address": "address_11e449fee904",
                                       "addressDetails": "addressDetails_448b001ad5f4",
                                       "zip": "zip_04f04f5917ee",
                                       "municipality": "municipality_46fe98cae3b9",
                                       "municipalityDetails": "municipalityDetails_c1570a51370e",
                                       "province": "province_64c2cd50bbe7",
                                       "foreignState": "foreignState_56ffffde8ca8"
                                     },
                                     "generatedAarUrl": "generatedAarUrl_4501ccf81a28",
                                     "numberOfPages": 0,
                                     "reasonCode": "reasonCode_42435a391ed6",
                                     "reason": "reason_6637ca375582",
                                     "notificationCost": 0,
                                     "analogCost": 0,
                                     "sendingReceipts": [
                                       {
                                         "id": "id_4426364f1147",
                                         "system": "system_547ea90494af"
                                       }
                                     ],
                                     "eventCode": "eventCode_11c9ab59be67",
                                     "shouldRetry": false,
                                     "raddType": "raddType_dbf742ee67ea",
                                     "raddTransactionId": "raddTransactionId_1b566e924d33",
                                     "productType": "productType_7239f341e7b1",
                                     "requestTimelineId": "requestTimelineId_b149e34adf7e",
                                     "delegateInfo": {
                                       "internalId": "internalId_58a358d56f39",
                                       "operatorUuid": "operatorUuid_65f56a029df8",
                                       "mandateId": "mandateId_409a4fabe859",
                                       "delegateType": "PF"
                                     },
                                     "legalFactId": "legalFactId_b39b2086a3cf",
                                     "aarKey": "aarKey_ae6e5d02d1e0",
                                     "eventTimestamp": "2024-02-09T15:30:00Z",
                                     "completionWorkflowDate": "2024-02-09T15:30:00Z",
                                     "endWorkflowStatus": "endWorkflowStatus_88569f6c3970",
                                     "recipientType": "recipientType_3fc394294f36",
                                     "amount": 0,
                                     "creditorTaxId": "creditorTaxId_e1cb817ee8b7",
                                     "noticeCode": "noticeCode_9099cb3a70fe",
                                     "paymentSourceChannel": "paymentSourceChannel_199add454ab5",
                                     "schedulingDate": "2024-02-09T15:30:00Z",
                                     "ioSendMessageResult": "NOT_SENT_OPTIN_ALREADY_SENT",
                                     "envelopeWeight": 0,
                                     "f24Attachments": [
                                       "f24Attachments_b223cf16118b"
                                     ],
                                     "deliveryDetailCode": "deliveryDetailCode_ed6da42d1de3",
                                     "deliveryFailureCause": "deliveryFailureCause_e3480e2f0a70",
                                     "attachments": [
                                       {
                                         "id": "id_aab5b1288377",
                                         "documentType": "documentType_8ff6b07f8d58",
                                         "url": "url_d6c089b02036",
                                         "date": "2024-02-09T15:30:00Z"
                                       }
                                     ],
                                     "prepareRequestId": "prepareRequestId_46aa94488b6a",
                                     "sendRequestId": "sendRequestId_39afd66d360c",
                                     "isFirstSendRetry": false,
                                     "relatedFeedbackTimelineId": "relatedFeedbackTimelineId_fa41fbb62feb",
                                     "nextDigitalAddressSource": "PLATFORM",
                                     "nextSourceAttemptsMade": 0,
                                     "nextLastAttemptMadeForSource": "2024-02-09T15:30:00Z",
                                     "refusalReasons": [
                                       {
                                         "errorCode": "errorCode_4dbe6f445b3b",
                                         "detail": "detail_4f704c54e7b7"
                                       }
                                     ],
                                     "uncertainPaymentDate": false,
                                     "legalFactGenerationDate": "2024-02-09T15:30:00Z",
                                     "registeredLetterCode": "registeredLetterCode_3a5e8b0bfa0f",
                                     "schedulingAnalogDate": "2024-02-09T15:30:00Z",
                                     "cancellationRequestId": "cancellationRequestId_f45546ecc011",
                                     "notRefinedRecipientIndexes": [
                                       0
                                     ],
                                     "failureCause": "failureCause_7e394b12c845"
                                   }
            }
        """;
        TimelineElementEntity timelineElementEntity = converter.jsonToEntity(jsonResult);

        assertNotNull(timelineElementEntity);
        assertNotNull(timelineElementEntity.getDetails());
    }
}