package org.example.simplemailclient.util;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

public class MailUtil {

    public static void printAllFolders(Store store) {
        try {
            Folder[] folders = store.getDefaultFolder().list("*");
            for (Folder folder : folders) {
                System.out.println("Folder: " + folder.getName());
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}