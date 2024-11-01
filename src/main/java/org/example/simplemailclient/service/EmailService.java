package org.example.simplemailclient.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import org.example.simplemailclient.dto.EmailRequest;
import org.example.simplemailclient.dto.EmailResponse;
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

    private final static int EMAIL_FETCH_LIMIT = 3;

    public static final String FOLDER_INBOX = "INBOX";
    public static final String FOLDER_OUTBOX = "[Gmail]/Sent Mail";
    public static final String FOLDER_TRASH = "[Gmail]/Trash";
    public static final String FOLDER_DRAFTS = "[Gmail]/Drafts";

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
        return fetchEmailsFromFolder(FOLDER_INBOX);
    }

    public String fetchOutbox() {
        return fetchEmailsFromFolder(FOLDER_OUTBOX);
    }

    public String fetchTrash() {
        return fetchEmailsFromFolder(FOLDER_TRASH);
    }

    public String fetchDrafts() {
        return fetchEmailsFromFolder(FOLDER_DRAFTS);
    }

    private String fetchEmailsFromFolder(String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName);
            Message[] messages = folder.getMessages();
            List<EmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - EMAIL_FETCH_LIMIT, 0); i--) {
                EmailResponse email = createEmailResponse(messages[i], folder);
                emailList.add(email);
            }

            folder.close(false);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);

        } catch (MessagingException e) {
            throw new RuntimeException("Error fetching emails from " + folderName + ": " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON for folder " + folderName + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String fetchInboxUnread() {
        try {
            IMAPFolder folder = openFolder("INBOX");

            FlagTerm unseenFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

            Message[] messages = folder.search(unseenFlagTerm);
            List<EmailResponse> emailList = new ArrayList<>();

            for (int i = messages.length - 1; i >= Math.max(messages.length - EMAIL_FETCH_LIMIT, 0); i--) {
                EmailResponse email = createEmailResponse(messages[i], folder);
                emailList.add(email);
            }

            folder.close(false);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(emailList);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching unread inbox emails: " + e.getMessage(), e);
        }
    }


    public String getEmailByUidInInbox(long uid) {
        return getEmailByUid(uid, FOLDER_INBOX);
    }

    public String getEmailByUidInOutbox(long uid) {
        return getEmailByUid(uid, FOLDER_OUTBOX);
    }

    public String getEmailByUidInTrash(long uid) {
        return getEmailByUid(uid, FOLDER_TRASH);
    }

    public String getEmailByUidInDrafts(long uid) {
        return getEmailByUid(uid, FOLDER_DRAFTS);
    }

    public String getEmailByUid(long uid, String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName);
            Message message = folder.getMessageByUID(uid);
            EmailResponse email = createEmailResponse(message, folder);

            folder.close(false);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(email);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching email by UID: " + e.getMessage(), e);
        }
    }

    private IMAPFolder openFolder(String folderName) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(properties);
        IMAPStore store = (IMAPStore) session.getStore("imaps");
        store.connect("imap.gmail.com", username, password);

        MailUtil.printAllFolders(store);

        IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);
        return folder;
    }

    private EmailResponse createEmailResponse(Message message, IMAPFolder folder) throws MessagingException, IOException {
        EmailResponse email = new EmailResponse();
        long uid = folder.getUID(message);
        email.setUid(uid);
        email.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
        email.setTo(getAddressesAsString(message.getRecipients(Message.RecipientType.TO)));
        email.setCc(getAddressesAsString(message.getRecipients(Message.RecipientType.CC)));
        email.setBcc(getAddressesAsString(message.getRecipients(Message.RecipientType.BCC)));
        email.setSubject(message.getSubject());
        email.setSentDate(message.getSentDate() != null ? message.getSentDate().toString() : null);
        email.setReceivedDate(message.getReceivedDate() != null ? message.getReceivedDate().toString() : null);
        email.setAttachments(getAttachments(message));
        return email;
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