package com.applab.applab_backend.email.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String to;
    private String subject;
    private String html;
}
