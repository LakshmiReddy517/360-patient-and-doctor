package com.pcare.patient.service;

import com.pcare.common.exception.BadRequestException;
import com.pcare.common.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores uploaded document binaries on the local file system (dev store on D:). */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.storage.uploads-dir:./uploads}") String uploadsDir) {
        this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create uploads directory " + root, e);
        }
    }

    /** Saves the file under a random name and returns the stored (relative) file name. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Empty file");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID() + ext;
        try {
            Path target = root.resolve(stored).normalize();
            if (!target.getParent().equals(root)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return stored;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }

    public Resource load(String storedFileName) {
        try {
            Path file = root.resolve(storedFileName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("File not found: " + storedFileName);
            }
            return resource;
        } catch (Exception e) {
            throw new NotFoundException("File not found: " + storedFileName);
        }
    }
}
