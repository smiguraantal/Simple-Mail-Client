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

    @GetMapping("/fetch/{uid}")
    public String getEmailByUid(@PathVariable("uid") long uid) {
        return emailService.getEmailByUid(uid);
    }
}