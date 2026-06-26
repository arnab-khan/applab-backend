package com.applab.applab_backend.auth.service;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.applab.applab_backend.auth.dto.GuestSessionExistsResponse;
import com.applab.applab_backend.auth.model.GuestSessionModel;
import com.applab.applab_backend.auth.repository.GuestSessionRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestSessionService {
    private final GuestSessionRepository guestSessionRepository;

    public GuestSessionExistsResponse createGuestSession(String guestId, HttpServletResponse response) {
        if (guestId != null) {
            GuestSessionModel existingGuestSession = guestSessionRepository.findByGuestId(guestId).orElse(null);
            if (existingGuestSession != null) {
                return new GuestSessionExistsResponse(true, existingGuestSession.getId());
            }
        }

        GuestSessionModel guestSession = new GuestSessionModel();
        guestSession.setGuestId(UUID.randomUUID().toString());
        GuestSessionModel savedGuestSession = guestSessionRepository.save(guestSession);

        ResponseCookie guestIdCookie = ResponseCookie.from("guestId", savedGuestSession.getGuestId())
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, guestIdCookie.toString());

        return new GuestSessionExistsResponse(true, savedGuestSession.getId());
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
