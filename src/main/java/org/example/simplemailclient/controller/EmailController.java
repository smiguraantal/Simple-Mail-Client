package org.example.simplemailclient.controller;

import org.example.simplemailclient.dto.EmailRequest;
import org.example.simplemailclient.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/folder")
    public String fetchEmailsFromFolder(@RequestParam("folderName") String folderName) {
        return emailService.fetchEmailsFromFolder(folderName);
    }

    @GetMapping("/fetch/{uid}")
    public String getEmailByUidInFolder(@PathVariable("uid") long uid, @RequestParam("folderName") String folderName) {
        return emailService.getEmailByUidInFolder(uid, folderName);
    }

    @GetMapping("/folder/read-status")
    public String fetchEmailsFromFolderByReadStatus(@RequestParam("folder") String folder, @RequestParam("seen") boolean seen) {
        return emailService.fetchEmailsFromFolderByReadStatus(folder, seen);
    }

    @GetMapping("/fetch/html-content/{uid}")
    public String getHtmlContentByUid(@PathVariable("uid") long uid, @RequestParam("folderName") String folderName) {
        return emailService.getHtmlContentByUid(uid, folderName);
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest);
        return ResponseEntity.ok("Email sent successfully!");
    }

    @PostMapping("/set-read-status/{uid}")
    public ResponseEntity<String> setReadStatus(
            @PathVariable("uid") long uid,
            @RequestParam("folder") String folder,
            @RequestParam("seen") boolean seen) {
        emailService.setReadStatus(uid, folder, seen);
        return ResponseEntity.ok("Email marked as " + (seen ? "read" : "unread"));
    }
}