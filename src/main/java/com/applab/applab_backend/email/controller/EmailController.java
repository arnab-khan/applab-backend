package com.applab.applab_backend.email.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.email.dto.EmailRequest;
import com.applab.applab_backend.email.service.EmailService;
import com.resend.core.exception.ResendException;

@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    // @PostMapping("/send")
    // public Map<String, String> sendEmail(@RequestBody EmailRequest request) throws ResendException {
    //     String emailId = emailService.sendEmail(request.getTo(), request.getSubject(), request.getHtml());
    //     return Map.of("id", emailId);
    // }
}
