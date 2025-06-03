package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.pnclients.RestTemplateFactory;
import it.pagopa.pn.deliverypush.MockAWSObjectsTest;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.config.msclient.ActionManagerApiConfigurator;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Collections;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTION_CONFLICT;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FUTURE_ACTION_NOTFOUND;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push.action-manager-base-url=http://localhost:9999"
})
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ActionManagerClientImpl.class
        , ActionManagerApiConfigurator.class,
        PnDeliveryPushConfigs.class,
        RestTemplateFactory.class})
class ActionManagerClientImplTest extends MockAWSObjectsTest {
    @Autowired
    private ActionManagerClientImpl actionManagerClient;

    private static ClientAndServer mockServer;

    @Test
    void unscheduleAction() {
        mockServer = ClientAndServer.startClientAndServer(9999); // usa la stessa porta

        try {
            String actionId = "action123";
            String timeSlot = "2023-10-01T10:00";
            String path = "/action-manager-private/action/" + timeSlot + "/" + actionId + "/unschedule";

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("PUT")
                            .withPath(path)
                    )
                    .respond(response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                    );

            actionManagerClient.unscheduleAction(actionId);
        } finally {
            mockServer.stop();
        }
    }
    @Test
    void unscheduleActionFailSilently() throws JsonProcessingException{
        mockServer = ClientAndServer.startClientAndServer(9999); // usa la stessa porta

        try {
            String actionId = "action123";
            String timeSlot = "2023-10-01T10:00";
            String path = "/action-manager-private/action/" + timeSlot + "/" + actionId + "/unschedule";

            ObjectMapper mapper = new ObjectMapper();


            ProblemError problemError = new ProblemError();
            problemError.setCode(ERROR_CODE_DELIVERYPUSH_FUTURE_ACTION_NOTFOUND);
            problemError.setDetail("");
            problemError.setElement("");

            Problem problem = new Problem();
            problem.setErrors(Collections.singletonList(problemError));


            String responseJson = mapper.writeValueAsString(problem);

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("PUT")
                            .withPath(path)
                    )
                    .respond(response()
                            .withStatusCode(404)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseJson)
                    );

            Assertions.assertDoesNotThrow(() -> actionManagerClient.unscheduleAction(actionId));
        } finally {
            mockServer.stop();
            mockServer.close();
        }
    }
    @Test
    void unscheduleActionFail() throws JsonProcessingException{
        mockServer = ClientAndServer.startClientAndServer(9999); // usa la stessa porta

        try {
            String actionId = "action123";
            String timeSlot = "2023-10-01T10:00";
            String path = "/action-manager-private/action/" + timeSlot + "/" + actionId + "/unschedule";

            ObjectMapper mapper = new ObjectMapper();


            ProblemError problemError = new ProblemError();
            problemError.setCode(ERROR_CODE_DELIVERYPUSH_FUTURE_ACTION_NOTFOUND);
            problemError.setDetail("");
            problemError.setElement("");

            Problem problem = new Problem();
            problem.setErrors(Collections.singletonList(problemError));


            String responseJson = mapper.writeValueAsString(problem);

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("PUT")
                            .withPath(path)
                    )
                    .respond(response()
                            .withStatusCode(409)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseJson)
                    );

            Assertions.assertThrows(Exception.class, () -> actionManagerClient.unscheduleAction(actionId));
        } finally {
            mockServer.stop();
        }
    }

    @Test
    void testAddOnlyActionIfAbsentSuccess() throws JsonProcessingException {
        mockServer = startClientAndServer(9999); // stessa porta del client

        try {
            NewAction actionRequest = new NewAction();
            actionRequest.setActionId("action456");
            actionRequest.setTimelineId("timeline123");
            actionRequest.setIun("iun123");
            actionRequest.setNotBefore(Instant.parse("2024-11-28T23:26:33.841637462Z"));
            actionRequest.setRecipientIndex(0);
            actionRequest.setType(it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType.valueOf(ActionType.ANALOG_WORKFLOW.name()));

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            String requestJson = mapper.writeValueAsString(actionRequest);

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("POST")
                            .withPath("/action-manager-private/action")
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withStatusCode(200)
                            .withContentType(MediaType.APPLICATION_JSON)
                    );
            actionManagerClient.addOnlyActionIfAbsent(actionRequest);
        } finally {
            mockServer.stop();
            mockServer.close();
        }
    }

    @Test
    void testAddOnlyActionIfAbsentFailSilently() throws JsonProcessingException {
        mockServer = startClientAndServer(9999); // stessa porta del client

        try {
            NewAction actionRequest = new NewAction();
            actionRequest.setActionId("action456");
            actionRequest.setTimelineId("timeline123");
            actionRequest.setIun("iun123");
            actionRequest.setNotBefore(Instant.parse("2024-11-28T23:26:33.841637462Z"));
            actionRequest.setRecipientIndex(0);
            actionRequest.setType(it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType.valueOf(ActionType.ANALOG_WORKFLOW.name()));

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            ProblemError problemError = new ProblemError();
            problemError.setCode(ERROR_CODE_DELIVERYPUSH_ACTION_CONFLICT);
            problemError.setDetail("");
            problemError.setElement("");

            Problem problem = new Problem();
            problem.setErrors(Collections.singletonList(problemError));

            String requestJson = mapper.writeValueAsString(actionRequest);

            String responseJson = mapper.writeValueAsString(problem);

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("POST")
                            .withPath("/action-manager-private/action")
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withStatusCode(409)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseJson)
                    );

            Assertions.assertDoesNotThrow(() -> actionManagerClient.addOnlyActionIfAbsent(actionRequest));
        } finally {
            mockServer.stop();
            mockServer.close();
        }
    }
    @Test
    void testAddOnlyActionIfAbsentFail() throws JsonProcessingException {
        mockServer = startClientAndServer(9999); // stessa porta del client

        try {
            NewAction actionRequest = new NewAction();
            actionRequest.setActionId("action456");
            actionRequest.setTimelineId("timeline123");
            actionRequest.setIun("iun123");
            actionRequest.setNotBefore(Instant.parse("2024-11-28T23:26:33.841637462Z"));
            actionRequest.setRecipientIndex(0);
            actionRequest.setType(it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType.valueOf(ActionType.ANALOG_WORKFLOW.name()));

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            ProblemError problemError = new ProblemError();
            problemError.setCode(ERROR_CODE_DELIVERYPUSH_ACTION_CONFLICT);
            problemError.setDetail("");
            problemError.setElement("");

            Problem problem = new Problem();
            problem.setErrors(Collections.singletonList(problemError));

            String requestJson = mapper.writeValueAsString(actionRequest);

            String responseJson = mapper.writeValueAsString(problem);

            new MockServerClient("localhost", 9999)
                    .when(request()
                            .withMethod("POST")
                            .withPath("/action-manager-private/action")
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withStatusCode(404)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(responseJson)
                    );

            Assertions.assertThrows(Exception.class, () -> actionManagerClient.addOnlyActionIfAbsent(actionRequest));

        } finally {
            mockServer.stop();
            mockServer.close();
        }
    }
}
