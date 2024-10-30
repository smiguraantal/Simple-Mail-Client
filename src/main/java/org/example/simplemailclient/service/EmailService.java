package org.example.simplemailclient.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.simplemailclient.dto.EmailRequest;
import org.example.simplemailclient.dto.InboxEmailResponse;
import org.example.simplemailclient.dto.OutboxEmailResponse;
import org.example.simplemailclient.exception.EmailSendingException;
import org.example.simplemailclient.util.MailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getText(), false);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send email");
        }
    }

    public String fetchInbox() {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            MailUtil.printAllFolders(store);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            List<InboxEmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - 3, 0); i--) {
                Message message = messages[i];
                InboxEmailResponse email = new InboxEmailResponse();

                String[] messageId = message.getHeader("Message-ID");
                email.setId(messageId[0]);

                email.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
                email.setSubject(message.getSubject());
                email.setDate(message.getSentDate().toString());
                emailList.add(email);
            }

            inbox.close(false);
            store.close();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);
        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        }
    }

    public String fetchOutbox() {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            MailUtil.printAllFolders(store);

            Folder inbox = store.getFolder("[Gmail]/Sent Mail");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            List<OutboxEmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - 3, 0); i--) {
                Message message = messages[i];
                OutboxEmailResponse email = new OutboxEmailResponse();

                String[] messageId = message.getHeader("Message-ID");
                email.setId(messageId[0]);

                email.setTo(((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress());
                email.setSubject(message.getSubject());
                email.setDate(message.getSentDate().toString());
                emailList.add(email);
            }

            inbox.close(false);
            store.close();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);
        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        }
    }
}