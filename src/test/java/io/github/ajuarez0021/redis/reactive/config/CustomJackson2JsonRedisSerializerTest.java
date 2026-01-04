package io.github.ajuarez0021.redis.reactive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class CustomJackson2JsonRedisSerializerTest.
 */
class CustomJackson2JsonRedisSerializerTest {

    /** The object mapper. */
    private ObjectMapper objectMapper;
    
    /** The serializer. */
    private CustomJackson2JsonRedisSerializer<TestPerson> serializer;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        serializer = new CustomJackson2JsonRedisSerializer<>(objectMapper, TestPerson.class);
    }

    /**
     * Serialize valid object returns bytes.
     */
    @Test
    void serialize_ValidObject_ReturnsBytes() {
        TestPerson person = new TestPerson("John", 30);

        byte[] result = serializer.serialize(person);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    /**
     * Serialize null value returns empty array.
     */
    @Test
    void serialize_NullValue_ReturnsEmptyArray() {
        TestPerson person = null;

        byte[] result = serializer.serialize(person);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    /**
     * Deserialize valid bytes returns object.
     */
    @Test
    void deserialize_ValidBytes_ReturnsObject() {
        TestPerson person = new TestPerson("John", 30);
        byte[] bytes = serializer.serialize(person);

        TestPerson result = serializer.deserialize(bytes);

        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    /**
     * Deserialize null bytes returns null.
     */
    @Test
    void deserialize_NullBytes_ReturnsNull() {
        byte[] bytes = null;

        TestPerson result = serializer.deserialize(bytes);

        assertNull(result);
    }

    /**
     * Deserialize empty bytes returns null.
     */
    @Test
    void deserialize_EmptyBytes_ReturnsNull() {
        byte[] bytes = new byte[0];

        TestPerson result = serializer.deserialize(bytes);

        assertNull(result);
    }

    /**
     * Serialize deserialize round trip preserves data.
     */
    @Test
    void serializeDeserialize_RoundTrip_PreservesData() {
        TestPerson original = new TestPerson("Jane Doe", 25);

        byte[] serialized = serializer.serialize(original);
        TestPerson deserialized = serializer.deserialize(serialized);

        assertNotNull(deserialized);
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getAge(), deserialized.getAge());
    }

    /**
     * Serialize complex object succeeds.
     */
    @Test
    void serialize_ComplexObject_Succeeds() {
        CustomJackson2JsonRedisSerializer<ComplexObject> complexSerializer =
                new CustomJackson2JsonRedisSerializer<>(objectMapper, ComplexObject.class);

        TestPerson person = new TestPerson("John", 30);
        ComplexObject complex = new ComplexObject(person, "Additional data");

        byte[] result = complexSerializer.serialize(complex);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    /**
     * Deserialize complex object succeeds.
     */
    @Test
    void deserialize_ComplexObject_Succeeds() {
        CustomJackson2JsonRedisSerializer<ComplexObject> complexSerializer =
                new CustomJackson2JsonRedisSerializer<>(objectMapper, ComplexObject.class);

        TestPerson person = new TestPerson("John", 30);
        ComplexObject original = new ComplexObject(person, "Additional data");
        byte[] serialized = complexSerializer.serialize(original);

        ComplexObject result = complexSerializer.deserialize(serialized);

        assertNotNull(result);
        assertEquals("John", result.getPerson().getName());
        assertEquals(30, result.getPerson().getAge());
        assertEquals("Additional data", result.getMetadata());
    }

    /**
     * The Class TestPerson.
     */
    static class TestPerson {
        
        /** The name. */
        private String name;
        
        /** The age. */
        private int age;

        /**
         * Instantiates a new test person.
         */
        public TestPerson() {}

        /**
         * Instantiates a new test person.
         *
         * @param name the name
         * @param age the age
         */
        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        /**
         * Gets the name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the new name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the age.
         *
         * @return the age
         */
        public int getAge() {
            return age;
        }

        /**
         * Sets the age.
         *
         * @param age the new age
         */
        public void setAge(int age) {
            this.age = age;
        }
    }

    /**
     * The Class ComplexObject.
     */
    static class ComplexObject {
        
        /** The person. */
        private TestPerson person;
        
        /** The metadata. */
        private String metadata;

        /**
         * Instantiates a new complex object.
         */
        public ComplexObject() {}

        /**
         * Instantiates a new complex object.
         *
         * @param person the person
         * @param metadata the metadata
         */
        public ComplexObject(TestPerson person, String metadata) {
            this.person = person;
            this.metadata = metadata;
        }

        /**
         * Gets the person.
         *
         * @return the person
         */
        public TestPerson getPerson() {
            return person;
        }

        /**
         * Sets the person.
         *
         * @param person the new person
         */
        public void setPerson(TestPerson person) {
            this.person = person;
        }

        /**
         * Gets the metadata.
         *
         * @return the metadata
         */
        public String getMetadata() {
            return metadata;
        }

        /**
         * Sets the metadata.
         *
         * @param metadata the new metadata
         */
        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }
}
