package com.bakery.bakery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    public String saveFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Path uploadDir = Paths.get(System.getProperty("user.dir")).resolve(subDirectory);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalName = file.getOriginalFilename();
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";

        String fileName = UUID.randomUUID().toString() + extension;
        Files.write(uploadDir.resolve(fileName), file.getBytes());

        // Повертаємо відносний шлях для БД
        // Якщо subDirectory = "uploads/documents", шлях буде "/uploads/documents/uuid.jpg"
        return "/" + subDirectory.replace("\\", "/") + "/" + fileName;
    }
}