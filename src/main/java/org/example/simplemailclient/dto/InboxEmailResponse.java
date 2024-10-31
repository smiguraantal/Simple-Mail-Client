package org.example.simplemailclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxEmailResponse {

    private long uid;
    private String from;
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String sentDate;
    private String receivedDate;
    private List<String> attachments;
}