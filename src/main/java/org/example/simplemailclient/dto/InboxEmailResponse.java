package org.example.simplemailclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxEmailResponse {

    private String id;
    private String from;
    private String subject;
    private String date;
}