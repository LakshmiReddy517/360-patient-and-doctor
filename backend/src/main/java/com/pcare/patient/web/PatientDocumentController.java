package com.pcare.patient.web;

import com.pcare.patient.domain.DocumentType;
import com.pcare.patient.domain.MedicalDocument;
import com.pcare.patient.service.FileStorageService;
import com.pcare.patient.service.PatientService;
import com.pcare.patient.web.dto.PatientDtos.MedicalDocumentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Patient Documents", description = "Medical document upload, listing and download")
@RestController
@RequestMapping("/api/v1/patients/{patientId}/documents")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','AGENT')")
public class PatientDocumentController {

    private final PatientService patientService;
    private final FileStorageService fileStorage;

    public PatientDocumentController(PatientService patientService, FileStorageService fileStorage) {
        this.patientService = patientService;
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "List medical documents for a patient")
    @GetMapping
    public List<MedicalDocumentDto> list(@PathVariable Long patientId) {
        return patientService.listDocuments(patientId);
    }

    @Operation(summary = "Upload a medical document (multipart)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MedicalDocumentDto upload(@PathVariable Long patientId,
                                     @RequestParam(defaultValue = "OTHER") DocumentType type,
                                     @RequestParam(required = false) String title,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate,
                                     @RequestParam(required = false) String source,
                                     @RequestParam(required = false) String description,
                                     @RequestParam(defaultValue = "false") boolean shareable,
                                     @RequestPart(required = false) MultipartFile file) {
        MedicalDocument meta = new MedicalDocument();
        meta.setType(type);
        meta.setTitle(title);
        meta.setDocumentDate(documentDate);
        meta.setSource(source);
        meta.setDescription(description);
        meta.setShareable(shareable);
        return patientService.toDocumentDto(patientService.addDocument(patientId, meta, file));
    }

    @Operation(summary = "Download a document's file")
    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long patientId, @PathVariable Long documentId) {
        MedicalDocument doc = patientService.getDocument(documentId);
        if (doc.getStoredFileName() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileStorage.load(doc.getStoredFileName());
        String filename = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : "document";
        String ct = doc.getContentType() != null ? doc.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(ct))
                .body(resource);
    }
}
