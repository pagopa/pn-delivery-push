package it.pagopa.pn.deliverypush;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV28;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

public class CheckServerAndClientDetailsTest {
    private static final List<TestErrors> testErrors = new ArrayList<>();
    private static int counter = 0;

    @Test
    void testAllDetailClasses()  {

        classPairsProvider().forEach(pair -> {
            try {
                testClassPair(pair);
            } catch (InvocationTargetException | InstantiationException | NoSuchMethodException |
                     IllegalAccessException e) {
                throw new CheckServerAndClientDetailsTestException("Error while testing class pair: " + pair.serverClass().getSimpleName() + " and " + pair.clientClass().getSimpleName(), e);
            }
        });
        
        // Verifica che il numero di classi testate CheckServerAndClientDetails al numero di classi generate che implementano l'interfaccia TimelineElementDetailsV27
        // In caso l'asserzione fallisca, significa che la classe di test non è aggiornata con le classi generate dall'openapi.
        try {
            Assertions.assertEquals(TimelineDetailsServerCounter.countSubClasses(), counter, "The number of tested classes does not match the number of generated classes implementing TimelineElementDetailsV28");
        } catch (Exception e) {
            throw new CheckServerAndClientDetailsTestException("Error while counting generated classes", e);
        }
        
        Assertions.assertEquals(0, testErrors.size(), "There are errors in the comparison of internal and generated classes: \n" +
                String.join("\n", testErrors.stream().map(error ->
                                "[" + error.getInternalClassName() + " - " + error.getGeneratedClassName() + "] --> " + String.join(",",error.getFieldName()))
                        .toList()));
    }

    // Aggiungere qui le coppie di classi server e client generate da confrontare
    static Stream<Pair> classPairsProvider() {
        return Stream.of(
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SenderAckCreationRequestDetails.class, SenderAckCreationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ValidateNormalizeAddressDetails.class, ValidateNormalizeAddressDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NormalizedAddressDetails.class, NormalizedAddressDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationRequestAcceptedDetailsV28.class, NotificationRequestAcceptedDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendCourtesyMessageDetails.class, SendCourtesyMessageDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.GetAddressInfoDetails.class, GetAddressInfoDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PublicRegistryCallDetails.class, PublicRegistryCallDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PublicRegistryResponseDetails.class, PublicRegistryResponseDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ScheduleAnalogWorkflowDetailsV23.class, ScheduleAnalogWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ScheduleDigitalWorkflowDetailsV23.class, ScheduleDigitalWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PrepareDigitalDetails.class, PrepareDigitalDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalDetails.class, SendDigitalDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedbackDetails.class, SendDigitalFeedbackDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalProgressDetailsV23.class, SendDigitalProgressDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RefinementDetailsV23.class, RefinementDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ScheduleRefinementDetails.class, ScheduleRefinementDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalDeliveryCreationRequestDetailsV28.class, DigitalDeliveryCreationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalSuccessWorkflowDetails.class, DigitalSuccessWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalFailureWorkflowDetails.class, DigitalFailureWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AnalogSuccessWorkflowDetails.class, AnalogSuccessWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AnalogFailureWorkflowDetails.class, AnalogFailureWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CompletelyUnreachableCreationRequestDetailsV28.class, CompletelyUnreachableCreationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.BaseRegisteredLetterDetails.class, BaseRegisteredLetterDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SimpleRegisteredLetterDetails.class, SimpleRegisteredLetterDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationViewedCreationRequestDetailsV28.class, NotificationViewedCreationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationViewedDetailsV23.class, NotificationViewedDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.BaseAnalogDetails.class, BaseAnalogDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PrepareAnalogDomicileFailureDetails.class, PrepareAnalogDomicileFailureDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendAnalogDetails.class, SendAnalogDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendAnalogProgressDetailsV23.class, SendAnalogProgressDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendAnalogFeedbackDetailsV25.class, SendAnalogFeedbackDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationPaidDetailsV23.class, Set.of("idF24"), NotificationPaidDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CompletelyUnreachableDetails.class, CompletelyUnreachableDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestRefusedDetailsV27.class, RequestRefusedDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarCreationRequestDetails.class, AarCreationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarGenerationDetails.class, AarGenerationDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotHandledDetails.class, NotHandledDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SimpleRegisteredLetterProgressDetails.class, SimpleRegisteredLetterProgressDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableDateAnalogWorkflowDetails.class, ProbableDateAnalogWorkflowDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationCancellationRequestDetails.class, NotificationCancellationRequestDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationCancelledDetails.class, NotificationCancelledDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationRADDRetrievedDetails.class, NotificationRADDRetrievedDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AnalogWorkflowRecipientDeceasedDetailsV26.class, AnalogWorkflowRecipientDeceasedDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PublicRegistryValidationCallDetails.class, PublicRegistryValidationCallDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PublicRegistryValidationResponseDetails.class, PublicRegistryValidationResponseDetails.class),
                new Pair(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationTimelineReworkedDetails.class, NotificationTimelineReworkedDetails.class)
        );
    }

    private void testClassPair(Pair pair) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        counter++;
        TestErrors error = compareFields(pair);
        if(!CollectionUtils.isEmpty(error.getFieldName())) {
            testErrors.add(error);
        }
    }

    private TestErrors compareFields(Pair pair) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends TimelineElementDetailsV28> serverClass = pair.serverClass();
        Class<? extends TimelineElementDetails> clientClass = pair.clientClass();
        Object server = serverClass.getDeclaredConstructor().newInstance();
        Object client = clientClass.getDeclaredConstructor().newInstance();

        Set<String> fields1 = getFieldsInfo(server.getClass());
        Set<String> fieldsToExclude = pair.fieldsToExclude();
        fields1.removeAll(fieldsToExclude); // Rimuovi i campi server da escludere dalla verifica
        Set<String> fields2 = getFieldsInfo(client.getClass());

        Set<String> missingInSecond = new HashSet<>(fields1);
        missingInSecond.removeAll(fields2);

        TestErrors error = TestErrors.builder()
                .internalClassName(server.getClass().getSimpleName())
                .generatedClassName(client.getClass().getSimpleName())
                .build();

        if(!CollectionUtils.isEmpty(missingInSecond)) {
            error.setFieldName(missingInSecond);
        }
        return error;
    }

    private Set<String> getFieldsInfo(Class<?> clazz) {
        Set<String> fieldsInfo = new HashSet<>();

        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            allFields.addAll(Arrays.asList(fields));
            currentClass = currentClass.getSuperclass();
        }

        for (Field field : allFields) {
            if (!Modifier.isStatic(field.getModifiers()) &&
                    !Modifier.isFinal(field.getModifiers()) &&
                    !field.isSynthetic() &&
                    !field.getName().equalsIgnoreCase("categoryType")) {
                fieldsInfo.add(field.getName());
            }
        }

        return fieldsInfo;
    }

    @Builder
    @Getter
    @Setter
    public static class TestErrors {
        private Set<String> fieldName;
        private String internalClassName;
        private String generatedClassName;
    }


    public record Pair(Class<? extends TimelineElementDetailsV28> serverClass, Set<String> fieldsToExclude ,Class<? extends TimelineElementDetails> clientClass) {
        public Pair(Class<? extends TimelineElementDetailsV28> serverClass, Class<? extends TimelineElementDetails> clientClass) {
            this(serverClass, Collections.emptySet(), clientClass);
        }
    }

    private static class CheckServerAndClientDetailsTestException extends RuntimeException {
        public CheckServerAndClientDetailsTestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
