package com.projectx.resumebuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/reformat-resume")
public class ResumeController {

    @Autowired
    private ChatGptService chatGptService;

    @PostMapping
    public ResponseEntity<String> reformatResume(
            @RequestParam("userResume") MultipartFile userResume,
            @RequestParam("templateResume") MultipartFile templateResume,
            @RequestParam("outputPath") String outputPath) {
        try {
            // Convert MultipartFile to File
            File userResumeFile = convertMultipartFileToFile(userResume);
            File templateResumeFile = convertMultipartFileToFile(templateResume);

            // Reformat the resume and save as PDF
            String result = chatGptService.reformatResumeToTemplate(
                    userResumeFile.getAbsolutePath(),
                    templateResumeFile.getAbsolutePath(),
                    outputPath // This will be the output PDF path
            );

            // Delete temporary files
            userResumeFile.delete();
            templateResumeFile.delete();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
