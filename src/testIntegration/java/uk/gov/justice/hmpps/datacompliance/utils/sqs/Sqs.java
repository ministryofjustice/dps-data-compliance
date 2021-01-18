package uk.gov.justice.hmpps.datacompliance.utils.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public abstract class Sqs {

    private ObjectMapper objectMapper;

     ObjectMapper getObjectMapper(){
         if(objectMapper == null){
             objectMapper = new ObjectMapper();
             objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
         }
         return objectMapper;
    }

}
