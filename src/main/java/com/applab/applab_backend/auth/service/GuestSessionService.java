package com.applab.applab_backend.auth.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.model.GuestSessionModel;
import com.applab.applab_backend.auth.repository.GuestSessionRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestSessionService {
    private final GuestSessionRepository guestSessionRepository;

    public void createGuestSession(HttpServletResponse response) {
        GuestSessionModel guestSession = new GuestSessionModel();
        guestSession.setGuestId(UUID.randomUUID().toString());
        GuestSessionModel savedGuestSession = guestSessionRepository.save(guestSession);

        Cookie guestIdCookie = new Cookie("guestId", savedGuestSession.getGuestId());
        guestIdCookie.setPath("/");
        response.addCookie(guestIdCookie);
    }
}
