package io.github.ajuarez0021.redis.reactive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class DefaultObjectMapperConfigTest.
 */
class DefaultObjectMapperConfigTest {

    /** The config. */
    private DefaultObjectMapperConfig config;
    
    /** The object mapper. */
    private ObjectMapper objectMapper;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        config = new DefaultObjectMapperConfig();
        objectMapper = config.configure();
    }

    /**
     * Configure returns configured object mapper.
     */
    @Test
    void configure_ReturnsConfiguredObjectMapper() {
        assertNotNull(objectMapper);
    }

    /**
     * Configure registers java time module.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_RegistersJavaTimeModule() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        TestObject obj = new TestObject(now);

        String json = objectMapper.writeValueAsString(obj);
        TestObject deserialized = objectMapper.readValue(json, TestObject.class);

        assertNotNull(deserialized.getDateTime());
    }

    /**
     * Configure registers jdk 8 module.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_RegistersJdk8Module() throws Exception {
        TestObjectWithOptional obj = new TestObjectWithOptional(Optional.of("value"));

        String json = objectMapper.writeValueAsString(obj);
        TestObjectWithOptional deserialized = objectMapper.readValue(json, TestObjectWithOptional.class);

        assertTrue(deserialized.getOptionalValue().isPresent());
        assertEquals("value", deserialized.getOptionalValue().get());
    }

    /**
     * Configure disables write dates as timestamps.
     */
    @Test
    void configure_DisablesWriteDatesAsTimestamps() {
        assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    /**
     * Configure does not fail on unknown properties.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_DoesNotFailOnUnknownProperties() throws Exception {
        String json = "{\"knownField\":\"value\",\"unknownField\":\"should not fail\"}";

        assertDoesNotThrow(() -> objectMapper.readValue(json, TestSimpleObject.class));
    }

    /**
     * Configure does not fail on empty beans.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_DoesNotFailOnEmptyBeans() throws Exception {
        EmptyBean emptyBean = new EmptyBean();

        assertDoesNotThrow(() -> objectMapper.writeValueAsString(emptyBean));
    }

    /**
     * Configure accepts single value as array.
     *
     * @throws Exception the exception
     */
    @Test
    void configure_AcceptsSingleValueAsArray() throws Exception {
        String json = "{\"values\":\"single_value\"}";

        TestObjectWithArray obj = objectMapper.readValue(json, TestObjectWithArray.class);

        assertNotNull(obj.getValues());
        assertEquals(1, obj.getValues().length);
        assertEquals("single_value", obj.getValues()[0]);
    }

    /**
     * The Class TestObject.
     */
    static class TestObject {
        
        /** The date time. */
        private LocalDateTime dateTime;

        /**
         * Instantiates a new test object.
         */
        public TestObject() {}

        /**
         * Instantiates a new test object.
         *
         * @param dateTime the date time
         */
        public TestObject(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        /**
         * Gets the date time.
         *
         * @return the date time
         */
        public LocalDateTime getDateTime() {
            return dateTime;
        }

        /**
         * Sets the date time.
         *
         * @param dateTime the new date time
         */
        public void setDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }
    }

    /**
     * The Class TestObjectWithOptional.
     */
    static class TestObjectWithOptional {
        
        /** The optional value. */
        private Optional<String> optionalValue;

        /**
         * Instantiates a new test object with optional.
         */
        public TestObjectWithOptional() {}

        /**
         * Instantiates a new test object with optional.
         *
         * @param optionalValue the optional value
         */
        public TestObjectWithOptional(Optional<String> optionalValue) {
            this.optionalValue = optionalValue;
        }

        /**
         * Gets the optional value.
         *
         * @return the optional value
         */
        public Optional<String> getOptionalValue() {
            return optionalValue;
        }

        /**
         * Sets the optional value.
         *
         * @param optionalValue the new optional value
         */
        public void setOptionalValue(Optional<String> optionalValue) {
            this.optionalValue = optionalValue;
        }
    }

    /**
     * The Class TestSimpleObject.
     */
    static class TestSimpleObject {
        
        /** The known field. */
        private String knownField;

        /**
         * Gets the known field.
         *
         * @return the known field
         */
        public String getKnownField() {
            return knownField;
        }

        /**
         * Sets the known field.
         *
         * @param knownField the new known field
         */
        public void setKnownField(String knownField) {
            this.knownField = knownField;
        }
    }

    /**
     * The Class EmptyBean.
     */
    static class EmptyBean {
        // Empty bean for testing
    }

    /**
     * The Class TestObjectWithArray.
     */
    static class TestObjectWithArray {
        
        /** The values. */
        private String[] values;

        /**
         * Gets the values.
         *
         * @return the values
         */
        public String[] getValues() {
            return values;
        }

        /**
         * Sets the values.
         *
         * @param values the new values
         */
        public void setValues(String[] values) {
            this.values = values;
        }
    }
}
