package com.microsoft.migration.assets.controller;

import com.microsoft.migration.assets.constants.StorageConstants;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/" + StorageConstants.STORAGE_PATH)
@RequiredArgsConstructor
@Slf4j
public class S3Controller {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    private final StorageService storageService;

    @GetMapping
    public String listObjects(Model model) {
        List<S3StorageItem> objects = storageService.listObjects();
        model.addAttribute("objects", objects);
        return "list";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadObject(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/" + StorageConstants.STORAGE_PATH + "/upload";
            }
            if (!isSupportedImageUpload(file)) {
                redirectAttributes.addFlashAttribute("error", "Only image files are allowed");
                return "redirect:/" + StorageConstants.STORAGE_PATH + "/upload";
            }

            storageService.uploadObject(file);
            redirectAttributes.addFlashAttribute("success", "File uploaded successfully");
            return "redirect:/" + StorageConstants.STORAGE_PATH;
        } catch (IOException e) {
            log.warn("Failed to upload file", e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload file");
            return "redirect:/" + StorageConstants.STORAGE_PATH + "/upload";
        }
    }
    
    @GetMapping("/view-page/{key}")
    public String viewObjectPage(@PathVariable String key, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Find the object in the list of objects
            Optional<S3StorageItem> foundObject = storageService.listObjects().stream()
                    .filter(obj -> obj.getKey().equals(key))
                    .findFirst();
            
            if (foundObject.isPresent()) {
                model.addAttribute("object", foundObject.get());
                return "view";
            } else {
                redirectAttributes.addFlashAttribute("error", "Image not found");
                return "redirect:/" + StorageConstants.STORAGE_PATH;
            }
        } catch (Exception e) {
            log.warn("Failed to view image '{}'", key, e);
            redirectAttributes.addFlashAttribute("error", "Failed to view image");
            return "redirect:/" + StorageConstants.STORAGE_PATH;
        }
    }

    @GetMapping("/view/{key}")
    public ResponseEntity<InputStreamResource> viewObject(@PathVariable String key) {
        try {
            InputStream inputStream = storageService.getObject(key);
            
            HttpHeaders headers = new HttpHeaders();
            // Use a generic content type if we don't know the exact type
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/delete/{key}")
    public String deleteObject(@PathVariable String key, RedirectAttributes redirectAttributes) {
        try {
            storageService.deleteObject(key);
            redirectAttributes.addFlashAttribute("success", "File deleted successfully");
        } catch (Exception e) {
            log.warn("Failed to delete file '{}'", key, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete file");
        }
        return "redirect:/" + StorageConstants.STORAGE_PATH;
    }

    private boolean isSupportedImageUpload(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        String lowerName = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }
}
