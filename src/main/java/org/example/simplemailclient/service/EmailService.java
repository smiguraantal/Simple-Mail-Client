package org.example.simplemailclient.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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

    public String fetchOutbox() {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            IMAPStore store = (IMAPStore) session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            MailUtil.printAllFolders(store);

            IMAPFolder outbox = (IMAPFolder) store.getFolder("[Gmail]/Sent Mail");
            outbox.open(Folder.READ_ONLY);

            Message[] messages = outbox.getMessages();
            List<OutboxEmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - 3, 0); i--) {
                Message message = messages[i];
                OutboxEmailResponse email = new OutboxEmailResponse();

                long uid = outbox.getUID(message);
                System.out.println("Az üzenet UID-ja: " + uid);

                email.setUid(uid);
                email.setTo(((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress());
                email.setSubject(message.getSubject());
                email.setDate(message.getSentDate().toString());
                emailList.add(email);
            }

            outbox.close(false);
            store.close();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);
        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        }
    }

    public String getEmailByUid(long uid) {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            IMAPStore store = (IMAPStore) session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            IMAPFolder allMailFolder = (IMAPFolder) store.getFolder("INBOX");
            if (!allMailFolder.isOpen()) {
                allMailFolder.open(Folder.READ_ONLY);
            }

            Message message = allMailFolder.getMessageByUID(uid);
            InboxEmailResponse email = new InboxEmailResponse();

            email.setUid(uid);
            email.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
            email.setTo(getAddressesAsString(message.getRecipients(Message.RecipientType.TO)));
            email.setCc(getAddressesAsString(message.getRecipients(Message.RecipientType.CC)));
            email.setBcc(getAddressesAsString(message.getRecipients(Message.RecipientType.BCC)));
            email.setSubject(message.getSubject());
            email.setSentDate(message.getSentDate() != null ? message.getSentDate().toString() : null);
            email.setReceivedDate(message.getReceivedDate() != null ? message.getReceivedDate().toString() : null);
            email.setAttachments(getAttachments(message));

            allMailFolder.close(false);
            store.close();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(email);
        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String fetchInbox() {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(properties);
            IMAPStore store = (IMAPStore) session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            MailUtil.printAllFolders(store);

            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            List<InboxEmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - 3, 0); i--) {
                Message message = messages[i];
                InboxEmailResponse email = new InboxEmailResponse();

                long uid = inbox.getUID(message);

                email.setUid(uid);
                email.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
                email.setTo(getAddressesAsString(message.getRecipients(Message.RecipientType.TO)));
                email.setCc(getAddressesAsString(message.getRecipients(Message.RecipientType.CC)));
                email.setBcc(getAddressesAsString(message.getRecipients(Message.RecipientType.BCC)));
                email.setSubject(message.getSubject());
                email.setSentDate(message.getSentDate() != null ? message.getSentDate().toString() : null);
                email.setReceivedDate(message.getReceivedDate() != null ? message.getReceivedDate().toString() : null);
                email.setAttachments(getAttachments(message));

                emailList.add(email);
            }

            inbox.close(false);
            store.close();

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);
        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        }
    }

    private String getAddressesAsString(Address[] addresses) {
        if (addresses == null) return null;
        return Arrays.stream(addresses)
                .map(address -> ((InternetAddress) address).getAddress())
                .collect(Collectors.joining(", "));
    }

    private List<String> getAttachments(Message message) throws MessagingException, IOException {
        List<String> attachments = new ArrayList<>();
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    attachments.add(bodyPart.getFileName());
                }
            }
        }
        return attachments;
    }
}