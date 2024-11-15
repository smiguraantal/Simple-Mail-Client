package org.example.simplemailclient.controller;

import org.example.simplemailclient.dto.DeleteRequest;
import org.example.simplemailclient.dto.EmailRequest;
import org.example.simplemailclient.dto.EmailResponse;
import org.example.simplemailclient.dto.ReadStatusUpdateRequest;
import org.example.simplemailclient.enumeration.SearchField;
import org.example.simplemailclient.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // ------------- GET ENDPOINTS -------------

    @GetMapping("/folder")
    public String fetchEmailsFromFolder(
            @RequestParam("folderName") String folderName) {
        return emailService.fetchEmailsFromFolder(folderName);
    }

    @GetMapping("/fetch/{uid}")
    public String getEmailByUidInFolder(
            @PathVariable("uid") long uid,
            @RequestParam("folderName") String folderName) {
        return emailService.getEmailByUidInFolder(uid, folderName);
    }

    @GetMapping("/folder/read-status")
    public String fetchEmailsFromFolderByReadStatus(
            @RequestParam("folder") String folder,
            @RequestParam("seen") boolean seen) {
        return emailService.fetchEmailsFromFolderByReadStatus(folder, seen);
    }

    @GetMapping("/fetch/html-content/{uid}")
    public String getHtmlContentByUid(
            @PathVariable("uid") long uid,
            @RequestParam("folderName") String folderName) {
        return emailService.getHtmlContentByUid(uid, folderName);
    }

    @GetMapping("/save-attachment")
    public void saveAttachment(
            @RequestParam("folderName") String folderName,
            @RequestParam("uid") long uid,
            @RequestParam("attachmentIndex") int attachmentIndex) {
        emailService.saveAttachment(folderName, uid, attachmentIndex);
    }

    @GetMapping("/save-all-attachments")
    public void saveAllAttachments(
            @RequestParam("folderName") String folderName,
            @RequestParam("uid") long uid) {
        emailService.saveAllAttachments(folderName, uid);
    }

    @GetMapping("/search")
    public List<EmailResponse> searchEmails(
            @RequestParam("folderName") String folderName,
            @RequestParam("keyword") String keyword,
            @RequestParam("searchField") SearchField searchField) {
        return emailService.searchEmails(folderName, keyword, searchField);
    }

    // ------------- POST ENDPOINTS -------------

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest);
        return ResponseEntity.ok("Email sent successfully!");
    }

    @PostMapping("/update-read-status/{uid}")
    public ResponseEntity<String> updateReadStatus(
            @PathVariable("uid") long uid,
            @RequestParam("folder") String folder,
            @RequestParam("seen") boolean seen) {
        emailService.updateReadStatus(uid, folder, seen);
        return ResponseEntity.ok("Email marked as " + (seen ? "read" : "unread"));
    }

    @PostMapping("/update-read-status-multiple")
    public ResponseEntity<String> updateReadStatusForMultipleMessages(
            @RequestBody ReadStatusUpdateRequest request) {
        emailService.updateReadStatusForMultipleMessages(request.getUids(), request.getFolderName(), request.isSeen());
        return ResponseEntity.ok("Read status updated successfully for specified messages.");
    }

    // ------------- DELETE ENDPOINTS -----------

    @DeleteMapping("/delete/{uid}")
    public String deleteEmail(
            @PathVariable("uid") long uid,
            @RequestParam("folderName") String folderName) {
        return emailService.deleteEmailByUID(uid, folderName);
    }

    @DeleteMapping("/delete-multiple")
    public String deleteEmails(
            @RequestBody DeleteRequest deleteRequest) {
        return emailService.deleteEmailsByUIDs(deleteRequest.getUids(), deleteRequest.getFolderName());
    }
}