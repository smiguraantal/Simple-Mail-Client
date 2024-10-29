package org.example.controller;

import jakarta.mail.MessagingException;
import org.example.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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
    public void sendEmail(@RequestParam String to,
                            @RequestParam String subject,
                            @RequestParam String text) throws MessagingException {
        emailService.sendEmail(to, subject, text);
    }
}