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

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest);
        return ResponseEntity.ok("Email sent successfully!");
    }

    @GetMapping("/inbox")
    public String fetchInbox() {
        return emailService.fetchInbox();
    }

    @GetMapping("/outbox")
    public String fetchOutbox() {
        return emailService.fetchOutbox();
    }

    @GetMapping("/trash")
    public String fetchTrash() {
        return emailService.fetchTrash();
    }

    @GetMapping("/drafts")
    public String fetchDrafts() {
        return emailService.fetchDrafts();
    }

    @GetMapping("/fetch/inbox/{uid}")
    public String getEmailByUidInInbox(@PathVariable("uid") long uid) {
        return emailService.getEmailByUidInInbox(uid);
    }

    @GetMapping("/fetch/outbox/{uid}")
    public String getEmailByUidInOutbox(@PathVariable("uid") long uid) {
        return emailService.getEmailByUidInOutbox(uid);
    }

    @GetMapping("/fetch/trash/{uid}")
    public String getEmailByUidInTrash(@PathVariable("uid") long uid) {
        return emailService.getEmailByUidInTrash(uid);
    }

    @GetMapping("/fetch/drafts/{uid}")
    public String getEmailByUidInDrafts(@PathVariable("uid") long uid) {
        return emailService.getEmailByUidInDrafts(uid);
    }

    @GetMapping("/inbox/status")
    public String fetchInboxStatus(@RequestParam("isRead") boolean isRead) {
        return emailService.fetchInboxStatus(isRead);
    }

    @GetMapping("/emails")
    public String fetchEmailsFromFolderByReadStatus(
            @RequestParam("folder") String folder,
            @RequestParam("isRead") boolean isRead) {
        return emailService.fetchEmailsFromFolderByReadStatus(folder, isRead);
    }

    @GetMapping("/fetch/inbox/text/{uid}")
    public String getTextByUidInInbox(@PathVariable("uid") long uid) {
        return emailService.getTextByUidInInbox(uid);
    }
}