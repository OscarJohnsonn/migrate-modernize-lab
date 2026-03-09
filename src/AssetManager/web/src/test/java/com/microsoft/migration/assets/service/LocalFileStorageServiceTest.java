package com.microsoft.migration.assets.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @TempDir
    Path tempDir;

    private LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService(rabbitTemplate);
        ReflectionTestUtils.setField(service, "rootLocation", tempDir.toAbsolutePath().normalize());
    }

    @Test
    void getObjectRejectsPathTraversal() {
        assertThrows(IOException.class, () -> service.getObject("../secret.txt"));
    }

    @Test
    void getObjectReadsFileWithinStorageRoot() throws Exception {
        Path storedFile = tempDir.resolve("image.jpg");
        Files.write(storedFile, "data".getBytes());

        try (InputStream inputStream = service.getObject("image.jpg")) {
            assertEquals('d', inputStream.read());
            assertTrue(inputStream.read() > -1);
        }
    }
}
