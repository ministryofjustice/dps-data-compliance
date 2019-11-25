package uk.gov.justice.hmpps.datacompliance.services.health;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import static org.assertj.core.api.Assertions.assertThat;

class HealthInfoTest {

    @Test
    void healthProvidesVersionInfo() {

        var properties = new Properties();
        properties.setProperty("version", "someVersion");

        assertThat(new HealthInfo(new BuildProperties(properties)).health().getDetails())
                .isEqualTo(Map.of("version", "someVersion"));
    }
}