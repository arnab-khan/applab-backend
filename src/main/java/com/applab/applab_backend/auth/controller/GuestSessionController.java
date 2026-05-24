package com.applab.applab_backend.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;

import com.applab.applab_backend.auth.service.GuestSessionService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestSessionController {
    private final GuestSessionService guestSessionService;

    @PostMapping("/create")
    public void createGuestSession(@CookieValue(value = "guestId", required = false) String guestId,
            HttpServletResponse response) {
        guestSessionService.createGuestSession(guestId, response);
    }

    @GetMapping("/exists")
    public boolean hasGuestSession(@CookieValue(value = "guestId", required = false) String guestId) {
        return guestSessionService.hasGuestSession(guestId);
    }
}
