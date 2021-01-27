package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public abstract class QueueFactory {

    public static final String STRING = "String";
    public static final String EVENT_TYPE = "eventType";
    public static final String CONTENT_TYPE = "contentType";

    public static ObjectMapper objectMapper;


   public static MessageAttributeValue stringAttribute(final String value) {
        return new MessageAttributeValue()
            .withDataType(STRING)
            .withStringValue(value);
    }


    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return objectMapper;
    }

    public static String asJson(Object value) {
        try {
            return getObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
