package org.example.simplemailclient.service;

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

    private final static int EMAIL_FETCH_LIMIT = 5;

    public static final String FOLDER_TRASH = "[Gmail]/Trash";

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);

            helper.setTo(emailRequest.getTo().toArray(new String[0]));

            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                helper.setCc(emailRequest.getCc().toArray(new String[0]));
            }

            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                helper.setBcc(emailRequest.getBcc().toArray(new String[0]));
            }

            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getText(), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send email");
        }
    }

    public String fetchEmailsFromFolder(String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            return convertMessagesToJson(messages, folder);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Error fetching emails from " + folderName + ": " + e.getMessage(), e);
        }
    }

    public String fetchEmailsFromFolderByReadStatus(String folderName, boolean seen) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_ONLY);
            FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), seen);
            Message[] messages = folder.search(flagTerm);
            return convertMessagesToJson(messages, folder);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Error fetching emails from " + folderName + ": " + e.getMessage(), e);
        }
    }

    private String convertMessagesToJson(Message[] messages, IMAPFolder folder) throws MessagingException, IOException {
        List<EmailResponse> emailList = new ArrayList<>();

        for (int i = messages.length - 1; i >= Math.max(messages.length - EMAIL_FETCH_LIMIT, 0); i--) {
            EmailResponse email = createEmailResponse(messages[i], folder);
            emailList.add(email);
        }

        folder.close(false);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(emailList);
    }

    public String getEmailByUidInFolder(long uid, String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_ONLY);
            Message message = folder.getMessageByUID(uid);
            EmailResponse email = createEmailResponse(message, folder);

            folder.close(false);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(email);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching email by UID: " + e.getMessage(), e);
        }
    }

    private IMAPFolder openFolder(String folderName, int folderAccessMode) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(properties);
        IMAPStore store = (IMAPStore) session.getStore("imaps");
        store.connect("imap.gmail.com", username, password);

        MailUtil.printAllFolders(store);

        IMAPFolder folder = (IMAPFolder) store.getFolder(folderName);
        folder.open(folderAccessMode);
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

    public String getHtmlContentByUid(long uid, String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_ONLY);
            Message message = folder.getMessageByUID(uid);

            String body = getHtmlContentFromMessage(message);

            folder.close(false);

            return body;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching email by UID: " + e.getMessage(), e);
        }
    }

    public String getHtmlContentFromMessage(Message message) {
        try {
            if (message.isMimeType("text/html")) {
                return (String) message.getContent();
            } else {
                throw new IllegalArgumentException("Message content is not HTML.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving HTML content from message: " + e.getMessage(), e);
        }
    }

    public void updateReadStatus(long uid, String folderName, boolean seen) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_WRITE);
            Message message = folder.getMessageByUID(uid);

            message.setFlag(Flags.Flag.SEEN, seen);

            folder.close(false);
        } catch (Exception e) {
            throw new RuntimeException("Error marking email as " + (seen ? "read" : "unread") + ": " + e.getMessage(), e);
        }
    }

    public void updateReadStatusForMultipleMessages(List<Long> uids, String folderName, boolean seen) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_WRITE);

            for (Long uid : uids) {
                Message message = folder.getMessageByUID(uid);
                message.setFlag(Flags.Flag.SEEN, seen);
            }

            folder.close(false);
        } catch (Exception e) {
            throw new RuntimeException("Error marking emails as " + (seen ? "read" : "unread") + ": " + e.getMessage(), e);
        }
    }


    public String deleteEmailByUID(long uid, String folderName) {
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_WRITE);
            Message message = folder.getMessageByUID(uid);

            if (message == null) return "Email not found.";

            IMAPFolder trashFolder = openFolder(FOLDER_TRASH, Folder.READ_WRITE);
            trashFolder.appendMessages(new Message[]{message});

            folder.expunge();
            folder.close(false);

            trashFolder.close(false);
            return "Email successfully moved to Trash.";

        } catch (MessagingException e) {
            return "Error while deleting email: " + e.getMessage();
        }
    }

    public String deleteEmailsByUIDs(List<Long> uids, String folderName) {
        StringBuilder result = new StringBuilder();
        try {
            IMAPFolder folder = openFolder(folderName, Folder.READ_WRITE);
            IMAPFolder trashFolder = openFolder(FOLDER_TRASH, Folder.READ_WRITE);

            for (Long uid : uids) {
                Message message = folder.getMessageByUID(uid);

                if (message == null) {
                    result.append("Email with UID ").append(uid).append(" not found.\n");
                    continue;
                }

                trashFolder.appendMessages(new Message[]{message});
            }

            folder.expunge();
            folder.close(false);
            trashFolder.close(false);

            return result.isEmpty() ? "Emails successfully moved to Trash." : result.toString();

        } catch (MessagingException e) {
            return "Error while deleting emails: " + e.getMessage();
        }
    }

}