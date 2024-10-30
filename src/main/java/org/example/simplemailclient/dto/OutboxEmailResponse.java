package org.example.simplemailclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEmailResponse {

    private String id;
    private String to;
    private String subject;
    private String date;
}
