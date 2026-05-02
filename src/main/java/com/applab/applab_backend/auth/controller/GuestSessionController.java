package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applab.applab_backend.auth.service.GuestSessionService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestSessionController {
    private final GuestSessionService guestSessionService;

    @PostMapping("/create")
    public void createGuestSession(HttpServletResponse response) {
        guestSessionService.createGuestSession(response);
    }
}
