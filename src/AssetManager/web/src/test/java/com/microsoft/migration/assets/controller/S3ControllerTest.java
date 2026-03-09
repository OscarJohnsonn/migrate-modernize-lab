package com.microsoft.migration.assets.controller;

import com.microsoft.migration.assets.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private S3Controller controller;

    @Test
    void uploadObjectRejectsNonImageFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.uploadObject(file, redirectAttributes);

        assertEquals("redirect:/storage/upload", view);
        assertEquals("Only image files are allowed", redirectAttributes.getFlashAttributes().get("error"));
        verify(storageService, never()).uploadObject(any());
    }

    @Test
    void uploadObjectDoesNotExposeInternalErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "hello".getBytes());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IOException("s3 timeout details")).when(storageService).uploadObject(any());

        String view = controller.uploadObject(file, redirectAttributes);

        assertEquals("redirect:/storage/upload", view);
        assertEquals("Failed to upload file", redirectAttributes.getFlashAttributes().get("error"));
        verify(storageService).uploadObject(any());
    }
}
