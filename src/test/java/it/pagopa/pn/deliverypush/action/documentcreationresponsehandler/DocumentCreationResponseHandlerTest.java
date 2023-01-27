package it.pagopa.pn.deliverypush.action.documentcreationresponsehandler;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.doThrow;

class DocumentCreationResponseHandlerTest {
    @Mock
    private ReceivedLegalFactCreationResponseHandler receivedLegalFactHandler;
    @Mock
    private AarCreationResponseHandler aarCreationResponseHandler;

    private DocumentCreationResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DocumentCreationResponseHandler(receivedLegalFactHandler, aarCreationResponseHandler);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseReceivedSenderAck() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = null;
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("legalFactId")
                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
                .build();
        
        //WHEN
        handler.handleResponseReceived(iun, recIndex, details);
        //THEN
        Mockito.verify(receivedLegalFactHandler).handleReceivedLegalFactCreationResponse(iun, details.getKey());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseReceivedAAR() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("legalFactId")
                .documentCreationType(DocumentCreationTypeInt.AAR)
                .build();

        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleResponseReceived(iun, recIndex, details));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseReceivedDIGITAL_DELIVERY() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("legalFactId")
                .documentCreationType(DocumentCreationTypeInt.DIGITAL_DELIVERY)
                .build();

        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleResponseReceived(iun, recIndex, details));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseReceivedRECIPIENT_ACCESS() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("legalFactId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .build();

        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleResponseReceived(iun, recIndex, details));
    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void handleResponseReceivedKo() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = null;
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("legalFactId")
                .documentCreationType(DocumentCreationTypeInt.SENDER_ACK)
                .build();


        doThrow(new RuntimeException("ex")).when(receivedLegalFactHandler).handleReceivedLegalFactCreationResponse(Mockito.any(), Mockito.any());

        //WHEN
        Assertions.assertThrows(RuntimeException.class, () -> {
            handler.handleResponseReceived(iun, recIndex, details);
        });
    }
}