package org.example.simplemailclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadStatusUpdateRequest {

    private List<Long> uids;
    private String folderName;
    private boolean seen;
}
