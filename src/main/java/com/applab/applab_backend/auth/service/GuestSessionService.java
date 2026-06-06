package com.applab.applab_backend.auth.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.dto.GuestSessionExistsResponse;
import com.applab.applab_backend.auth.model.GuestSessionModel;
import com.applab.applab_backend.auth.repository.GuestSessionRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestSessionService {
    private final GuestSessionRepository guestSessionRepository;

    public void createGuestSession(String guestId, HttpServletResponse response) {
        if (getGuestSessionId(guestId) != null) {
            return;
        }

        GuestSessionModel guestSession = new GuestSessionModel();
        guestSession.setGuestId(UUID.randomUUID().toString());
        GuestSessionModel savedGuestSession = guestSessionRepository.save(guestSession);

        Cookie guestIdCookie = new Cookie("guestId", savedGuestSession.getGuestId());
        guestIdCookie.setPath("/");
        response.addCookie(guestIdCookie);
    }

    public Long getGuestSessionId(String guestId) {
        if (guestId == null) {
            return null;
        }

        return guestSessionRepository.findByGuestId(guestId)
                .map(GuestSessionModel::getId)
                .orElse(null);
    }

    public GuestSessionExistsResponse hasGuestSession(String guestId) {
        Long guestSessionId = getGuestSessionId(guestId);
        return new GuestSessionExistsResponse(guestSessionId != null, guestSessionId);
    }
}
