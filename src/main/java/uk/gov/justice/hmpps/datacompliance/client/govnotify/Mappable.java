package uk.gov.justice.hmpps.datacompliance.client.govnotify;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

public interface Mappable {

    ObjectMapper objectMapper = new ObjectMapper().setPropertyNamingStrategy(SNAKE_CASE);

    default Map<String, Object> toMap() {
        return objectMapper.convertValue(this, Map.class);
    }


}
