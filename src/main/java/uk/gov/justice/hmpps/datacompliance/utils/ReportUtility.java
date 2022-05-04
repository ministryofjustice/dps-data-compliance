package uk.gov.justice.hmpps.datacompliance.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.hmpps.datacompliance.dto.UalOffender;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Component
public class ReportUtility {

    public static final String CSV = "csv";

    private final CsvMapper csvMapper;

    {
        csvMapper = buildCvsMapper();
    }

    public List<UalOffender> parseFromUalReport(final MultipartFile file) {
        validate(file);
        try {
            return csvReader(UalOffender.class).<UalOffender>readValues(file.getInputStream()).readAll();
        } catch (IOException e) {
            log.warn("Unable to parse file.", e);
            throw new HttpClientErrorException(BAD_REQUEST, String.format("Unable to parse file. Please provide a use a csv which adheres to the specified format. Allowed values are: %s", getAllowedFields()));
        }
    }

    private ObjectReader csvReader(final Class<?> type) {
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        return csvMapper.readerFor(type)
            .with(schema);
    }

    private void validate(final MultipartFile file) {
        final Resource resource = file.getResource();

        if (!(resource.exists() && resource.isReadable())) {
            throw new HttpClientErrorException(BAD_REQUEST, "Invalid file provided.");
        }
        if(!isCsvExtension(file.getOriginalFilename())){
            throw new HttpClientErrorException(BAD_REQUEST, "The file must be a CSV with the .csv extension type.");
        }
    }

    private CsvMapper buildCvsMapper() {
        CsvMapper csvMapper = configuredCsvMapper();
        csvMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return csvMapper;
    }

    private CsvMapper configuredCsvMapper() {
        return CsvMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
            .configure(DeserializationFeature.EAGER_DESERIALIZER_FETCH, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }

    private String getAllowedFields() {
        return Arrays.stream(UalOffender.class.getDeclaredFields()).map(Field::getName).collect(joining(", "));
    }

    private boolean isCsvExtension(final String fileName) {
        return StringUtils.equalsAnyIgnoreCase(FilenameUtils.getExtension(fileName), CSV);
    }
}
