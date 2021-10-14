package uk.gov.justice.hmpps.datacompliance.services.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvParser.Feature;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class CsvService {

    private final CsvMapper csvMapper;

    public CsvService() {
        this.csvMapper = configuredCsvMapper();
    }

    public byte[] toCsv(Page<?> elements) throws JsonProcessingException {

        if (elements == null || elements.isEmpty()) {
            return "".getBytes(StandardCharsets.UTF_8);
        }

        Class clazz = elements.getContent().get(0).getClass();
        CsvSchema csvSchema = csvMapper.schemaFor(clazz).withHeader();
        ObjectWriter objectWriter = csvMapper.writer().with(csvSchema);
        return objectWriter.writeValueAsBytes(elements.getContent());
    }


    private CsvMapper configuredCsvMapper() {
        final CsvMapper csvMapper = new CsvMapper();
        csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        csvMapper.enable(Feature.SKIP_EMPTY_LINES);
        csvMapper.enable(Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
        csvMapper.registerModule(new JavaTimeModule());
        csvMapper.findAndRegisterModules();
        return csvMapper;
    }

}

