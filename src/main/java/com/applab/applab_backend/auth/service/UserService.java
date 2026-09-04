package com.applab.applab_backend.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.applab.applab_backend.auth.dto.LoginRequest;
import com.applab.applab_backend.auth.dto.ForgotPasswordOtpRequest;
import com.applab.applab_backend.auth.dto.ForgotPasswordOtpVerificationRequest;
import com.applab.applab_backend.auth.dto.PasswordVerificationRequest;
import com.applab.applab_backend.auth.dto.PasswordResetRequest;
import com.applab.applab_backend.auth.dto.PasswordResetTokenResponse;
import com.applab.applab_backend.auth.dto.ProfileCredentialsUpdateRequest;
import com.applab.applab_backend.auth.dto.ProfileBasicsUpdateRequest;
import com.applab.applab_backend.auth.dto.SignupRequest;
import com.applab.applab_backend.auth.dto.UserListItemResponse;
import com.applab.applab_backend.auth.dto.UserProfileImageResponse;
import com.applab.applab_backend.auth.model.UserModel;
import com.applab.applab_backend.auth.repository.UserRepository;
import com.applab.applab_backend.auth.enums.ProfileImageType;
import com.applab.applab_backend.auth.enums.PasswordVerificationPurpose;
import com.applab.applab_backend.common.exception.ApiException;
import com.applab.applab_backend.email.dto.EmailOtpRequest;
import com.applab.applab_backend.email.dto.EmailOtpResponse;
import com.applab.applab_backend.email.dto.EmailOtpVerificationRequest;
import com.applab.applab_backend.email.service.EmailService;
import com.applab.applab_backend.storage.model.FileEntityModel;
import com.applab.applab_backend.storage.service.StorageService;
import com.resend.core.exception.ResendException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Duration EMAIL_OTP_EXPIRY = Duration.ofMinutes(10);
    private static final Duration EMAIL_CHANGE_EXPIRY = Duration.ofHours(1);
    private static final Duration PASSWORD_VERIFICATION_EXPIRY = Duration.ofHours(1);
    private static final long EMAIL_OTP_RESEND_COOLDOWN_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final int EMAIL_OTP_MAX_RESENDS = 3;
    private static final int EMAIL_OTP_DIGITS = 6;
    private static final Duration FORGOT_PASSWORD_REQUEST_EXPIRY = Duration.ofHours(1);
    private static final Duration PASSWORD_RESET_TOKEN_EXPIRY = Duration.ofMinutes(10);
    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;

    // Hashes the password and then saves the user in the database
    public UserModel createUser(SignupRequest userDetails, HttpServletRequest request) {
        UserModel user = new UserModel();
        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        userRepository.save(user);
        return apiResponse(user, request);
    }

    public UserModel loginUser(LoginRequest loginDetails, HttpServletRequest request) {

        String usernameOrEmail = loginDetails.getUsername().trim();
        String rawPassword = loginDetails.getPassword();

        UserModel user = userRepository.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(usernameOrEmail);
        }
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Username or email not found");
        }

        validatePassword(rawPassword, user);

        // Successful login, create session and return response
        return apiResponse(user, request);
    }

    public EmailOtpResponse sendForgotPasswordOtp(ForgotPasswordOtpRequest request)
            throws ResendException {
        String email = request.getEmail().trim().toLowerCase();
        UserModel user = userRepository.findByEmailIgnoreCase(email);
        long sentAt = System.currentTimeMillis();

        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EMAIL_NOT_REGISTERED",
                    "No account is registered with this email address");
        }

        String indexKey = forgotPasswordUserKey(user.getId());
        String requestId = stringRedisTemplate.opsForValue().get(indexKey);
        String requestKey = requestId == null ? null : forgotPasswordKey(requestId);
        int resendsUsed = 0;

        if (requestKey != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(requestKey))) {
            long lastSentAt = Long.parseLong(
                    stringRedisTemplate.opsForHash().get(requestKey, "lastSentAt").toString());
            if (sentAt - lastSentAt < EMAIL_OTP_RESEND_COOLDOWN_MILLIS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "FORGOT_PASSWORD_OTP_COOLDOWN",
                        "Please wait before requesting another OTP");
            }
            int resendCount = Integer.parseInt(
                    stringRedisTemplate.opsForHash().get(requestKey, "resendCount").toString());
            if (resendCount >= EMAIL_OTP_MAX_RESENDS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "FORGOT_PASSWORD_OTP_RESEND_LIMIT_REACHED", "OTP resend limit reached");
            }
            resendsUsed = resendCount + 1;
        } else {
            requestId = UUID.randomUUID().toString();
            requestKey = forgotPasswordKey(requestId);
        }

        String otp = generateEmailOtp();
        emailService.sendEmail(
                email,
                "Reset your AppLab password",
                "<p>Your AppLab password reset code is:</p><h1>" + otp
                        + "</h1><p>This code expires in 10 minutes.</p>");

        long otpExpiresAt = sentAt + EMAIL_OTP_EXPIRY.toMillis();
        stringRedisTemplate.opsForHash().put(requestKey, "userId", user.getId().toString());
        stringRedisTemplate.opsForHash().put(requestKey, "otp", passwordEncoder.encode(otp));
        stringRedisTemplate.opsForHash().put(requestKey, "otpExpiresAt", Long.toString(otpExpiresAt));
        stringRedisTemplate.opsForHash().put(requestKey, "lastSentAt", Long.toString(sentAt));
        stringRedisTemplate.opsForHash().put(requestKey, "resendCount", Integer.toString(resendsUsed));
        stringRedisTemplate.opsForHash().put(requestKey, "failedAttempts", "0");
        stringRedisTemplate.expire(requestKey, FORGOT_PASSWORD_REQUEST_EXPIRY);
        stringRedisTemplate.opsForValue().set(indexKey, requestId, FORGOT_PASSWORD_REQUEST_EXPIRY);

        return forgotPasswordOtpResponse(
                requestId, email, sentAt, EMAIL_OTP_MAX_RESENDS - resendsUsed);
    }

    public PasswordResetTokenResponse verifyForgotPasswordOtp(
            ForgotPasswordOtpVerificationRequest verificationRequest) {
        String requestKey = forgotPasswordKey(verificationRequest.getRequestId());
        Object storedUserId = stringRedisTemplate.opsForHash().get(requestKey, "userId");
        Object storedOtp = stringRedisTemplate.opsForHash().get(requestKey, "otp");
        Object storedOtpExpiresAt = stringRedisTemplate.opsForHash().get(requestKey, "otpExpiresAt");

        if (storedUserId == null || storedOtp == null || storedOtpExpiresAt == null
                || System.currentTimeMillis() >= Long.parseLong(storedOtpExpiresAt.toString())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FORGOT_PASSWORD_OTP_EXPIRED",
                    "OTP is invalid or expired");
        }

        if (!passwordEncoder.matches(verificationRequest.getOtp(), storedOtp.toString())) {
            int failedAttempts = Integer.parseInt(String.valueOf(
                    stringRedisTemplate.opsForHash().get(requestKey, "failedAttempts"))) + 1;
            stringRedisTemplate.opsForHash().put(requestKey, "failedAttempts", Integer.toString(failedAttempts));
            if (failedAttempts >= FORGOT_PASSWORD_MAX_ATTEMPTS) {
                stringRedisTemplate.delete(requestKey);
                stringRedisTemplate.delete(forgotPasswordUserKey(Long.valueOf(storedUserId.toString())));
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "FORGOT_PASSWORD_OTP_ATTEMPTS_EXCEEDED", "Too many incorrect OTP attempts");
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "FORGOT_PASSWORD_OTP_INCORRECT", "OTP is incorrect");
        }

        Long userId = Long.valueOf(storedUserId.toString());
        String resetToken = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(
                passwordResetKey(resetToken), userId.toString(), PASSWORD_RESET_TOKEN_EXPIRY);
        stringRedisTemplate.delete(requestKey);
        stringRedisTemplate.delete(forgotPasswordUserKey(userId));
        Instant expiresAt = Instant.now().plus(PASSWORD_RESET_TOKEN_EXPIRY);
        return new PasswordResetTokenResponse(
                resetToken, expiresAt, PASSWORD_RESET_TOKEN_EXPIRY.toSeconds());
    }

    @Transactional
    public Map<String, String> resetForgottenPassword(PasswordResetRequest request) {
        String resetKey = passwordResetKey(request.getResetToken());
        String storedUserId = stringRedisTemplate.opsForValue().get(resetKey);
        if (storedUserId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_EXPIRED",
                    "Password reset token is invalid or expired");
        }

        UserModel user = userRepository.findById(Long.valueOf(storedUserId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        stringRedisTemplate.delete(resetKey);
        return Map.of("message", "Password reset successfully");
    }

    private EmailOtpResponse forgotPasswordOtpResponse(
            String requestId, String sentTo, long sentAt, int remainingResends) {
        return new EmailOtpResponse(
                "OTP sent successfully",
                requestId,
                sentTo,
                Instant.ofEpochMilli(sentAt).plus(EMAIL_OTP_EXPIRY),
                EMAIL_OTP_EXPIRY.toSeconds(),
                EMAIL_OTP_DIGITS,
                Duration.ofMillis(EMAIL_OTP_RESEND_COOLDOWN_MILLIS).toSeconds(),
                Instant.ofEpochMilli(sentAt + EMAIL_OTP_RESEND_COOLDOWN_MILLIS),
                remainingResends);
    }

    private String forgotPasswordKey(String requestId) {
        return "forgot-password:" + requestId;
    }

    private String forgotPasswordUserKey(Long userId) {
        return "forgot-password-user:" + userId;
    }

    private String passwordResetKey(String resetToken) {
        return "password-reset:" + resetToken;
    }

    private UserModel apiResponse(UserModel user, HttpServletRequest request) {

        HttpSession existingSession = request.getSession(false); // Get existing session if exists
        if (existingSession != null) {
            existingSession.invalidate(); // Invalidate existing session
        }
        // Generating session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId()); // Store the logged-in user's ID in the session

        // Create Authentication object for Spring Security
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId(), // Use user ID or username (not full object)
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(auth);
        session.setAttribute("securityContext", SecurityContextHolder.getContext());

        // Return success response with user details and session info
        return user;
    }

    public boolean isUsernameExist(String username) {
        return userRepository.existsByUsername(username);
    }

    public void verifyPassword(PasswordVerificationRequest passwordDetails,
            HttpServletRequest request) {
        UserModel user = getUserBySession(request);
        validateCurrentPassword(passwordDetails.getCurrentPassword(), user);

        String passwordVerificationRedisKey = passwordVerificationKey(
                request.getSession(false).getId(), passwordDetails.getPurpose());
        stringRedisTemplate.delete(passwordVerificationRedisKey);
        stringRedisTemplate.opsForHash().put(passwordVerificationRedisKey, "verified", "true");
        stringRedisTemplate.expire(passwordVerificationRedisKey, PASSWORD_VERIFICATION_EXPIRY);
    }

    public EmailOtpResponse sendEmailOtp(EmailOtpRequest emailDetails, HttpServletRequest request)
            throws ResendException {
        UserModel user = getUserBySession(request);
        String sessionId = request.getSession(false).getId();
        String passwordVerificationRedisKey = passwordVerificationKey(
                sessionId, PasswordVerificationPurpose.CHANGE_EMAIL);
        String email = emailDetails.getEmail().trim().toLowerCase();
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(passwordVerificationRedisKey))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "PASSWORD_VERIFICATION_REQUIRED",
                    "Password verification is required before changing email");
        }

        if (user.getEmail() != null && email.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New email must be different from your current email");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email address is already linked to another account");
        }

        Object storedRequestId = stringRedisTemplate.opsForHash()
                .get(passwordVerificationRedisKey, "requestId");
        String requestId = storedRequestId == null
                ? UUID.randomUUID().toString()
                : storedRequestId.toString();
        String emailChangeRedisKey = emailChangeKey(requestId);
        int resendCount = 0;
        if (storedRequestId != null) {
            validateEmailChangeOwner(emailChangeRedisKey, user.getId(), sessionId);
            String storedEmail = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "email").toString();
            if (!email.equals(storedEmail)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Complete the existing email change before using another email");
            }
            long lastSentAt = Long.parseLong(
                    stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "lastSentAt").toString());
            if (System.currentTimeMillis() - lastSentAt < EMAIL_OTP_RESEND_COOLDOWN_MILLIS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_OTP_COOLDOWN",
                        "Please wait before requesting another OTP");
            }
            resendCount = Integer.parseInt(
                    stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "resendCount").toString());
            if (resendCount >= EMAIL_OTP_MAX_RESENDS) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_OTP_RESEND_LIMIT_REACHED",
                        "OTP resend limit reached");
            }
        }

        String otp = generateEmailOtp();
        emailService.sendEmail(
                email,
                "Verify your AppLab email",
                "<p>Your AppLab verification code is:</p><h1>" + otp
                        + "</h1><p>This code expires in 10 minutes.</p>");

        long sentAt = System.currentTimeMillis();
        long otpExpiresAt = sentAt + EMAIL_OTP_EXPIRY.toMillis();
        int resendsUsed = storedRequestId == null ? 0 : resendCount + 1;
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "userId", user.getId().toString());
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "sessionId", sessionId);
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "email", email);
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "otp", passwordEncoder.encode(otp));
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "otpExpiresAt", Long.toString(otpExpiresAt));
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "lastSentAt", Long.toString(sentAt));
        stringRedisTemplate.opsForHash().put(emailChangeRedisKey, "resendCount",
                Integer.toString(resendsUsed));
        stringRedisTemplate.expire(emailChangeRedisKey, EMAIL_CHANGE_EXPIRY);
        stringRedisTemplate.opsForHash().put(passwordVerificationRedisKey, "requestId", requestId);
        return new EmailOtpResponse(
                "OTP sent successfully",
                requestId,
                email,
                Instant.ofEpochMilli(sentAt).plus(EMAIL_OTP_EXPIRY),
                EMAIL_OTP_EXPIRY.toSeconds(),
                EMAIL_OTP_DIGITS,
                Duration.ofMillis(EMAIL_OTP_RESEND_COOLDOWN_MILLIS).toSeconds(),
                Instant.ofEpochMilli(sentAt + EMAIL_OTP_RESEND_COOLDOWN_MILLIS),
                EMAIL_OTP_MAX_RESENDS - resendsUsed);
    }

    @Transactional
    public UserModel verifyEmailOtp(EmailOtpVerificationRequest verificationDetails,
            HttpServletRequest request) {
        UserModel user = getUserBySession(request);
        String emailChangeRedisKey = emailChangeKey(verificationDetails.getRequestId());
        validateEmailChangeOwner(emailChangeRedisKey, user.getId(), request.getSession(false).getId());
        Object storedEmail = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "email");
        Object storedOtp = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "otp");
        Object storedOtpExpiresAt = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "otpExpiresAt");

        if (storedEmail == null || storedOtp == null || storedOtpExpiresAt == null
                || System.currentTimeMillis() >= Long.parseLong(storedOtpExpiresAt.toString())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_OTP_EXPIRED",
                    "OTP is invalid or expired");
        }
        if (!passwordEncoder.matches(verificationDetails.getOtp(), storedOtp.toString())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_OTP_INCORRECT", "OTP is incorrect");
        }
        String email = storedEmail.toString();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        user.setEmail(email);
        UserModel savedUser = userRepository.save(user);
        stringRedisTemplate.delete(emailChangeRedisKey);
        stringRedisTemplate.delete(passwordVerificationKey(
                request.getSession(false).getId(), PasswordVerificationPurpose.CHANGE_EMAIL));
        return savedUser;
    }

    private void validateEmailChangeOwner(String emailChangeRedisKey, Long userId, String sessionId) {
        Object storedUserId = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "userId");
        Object storedSessionId = stringRedisTemplate.opsForHash().get(emailChangeRedisKey, "sessionId");
        if (storedUserId == null || storedSessionId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_CHANGE_INVALID_OR_EXPIRED",
                    "Email change request is invalid or expired");
        }
        if (!userId.toString().equals(storedUserId.toString())
                || !sessionId.equals(storedSessionId.toString())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_CHANGE_SESSION_MISMATCH",
                    "Email change request does not belong to this session");
        }
    }

    private String emailChangeKey(String requestId) {
        return "email-change:" + requestId;
    }

    private String generateEmailOtp() {
        StringBuilder otp = new StringBuilder(EMAIL_OTP_DIGITS);
        for (int index = 0; index < EMAIL_OTP_DIGITS; index++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    private String passwordVerificationKey(String sessionId, PasswordVerificationPurpose purpose) {
        return "password-verification:" + sessionId + ":" + purpose.name();
    }

    public Page<UserListItemResponse> getAll(String keyword, Pageable pageable) {
        List<String> allowedSorts = List.of("createdAt", "updatedAt", "name", "username");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + allowedSorts);
            }
        }
        return userRepository.searchUsers(keyword, pageable)
                .map(user -> new UserListItemResponse(
                        user.getId(),
                        user.getName(),
                        user.getUsername(),
                        null,
                        user.getCreatedAt(),
                        null,
                        user.getProfileImageUrl(),
                        user.getCompressedProfileImageUrl()));
    }

    public UserListItemResponse getPublicUserByUsername(String username) {
        UserModel user = userRepository.findByUsername(username);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return new UserListItemResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getProfileImageUrl(),
                null);
    }

    @Transactional(readOnly = true)
    public FileEntityModel getPublicProfileImageByUserId(Long userId, boolean fullImage) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        FileEntityModel image = user.getProfileImage();
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not found");
        }

        byte[] imageData = fullImage ? image.getData() : image.getCompressedData();
        if (imageData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not available");
        }

        FileEntityModel response = new FileEntityModel();
        response.setId(image.getId());
        response.setFileName(image.getFileName());
        response.setFileType(image.getFileType());
        response.setData(imageData);
        return response;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getPublicProfileImageRawByUserId(Long userId) {
        return getPublicProfileImageRawByUserId(userId, true);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getPublicProfileImageRawByUserId(Long userId, boolean fullImage) {
        FileEntityModel image = getPublicProfileImageByUserId(userId, fullImage);
        byte[] imageData = image.getData();
        MediaType contentType = image.getFileType() != null
                ? MediaType.parseMediaType(image.getFileType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .contentType(contentType)
                .body(imageData);
    }

    @Transactional(readOnly = true)
    public List<UserProfileImageResponse> getPublicProfileImagesByUserIds(List<Long> userIds, boolean fullImage) {
        return userRepository.findAllById(userIds).stream()
                .filter(user -> user.getProfileImage() != null)
                .map(user -> buildProfileImageResponse(
                        user.getId(),
                        user.getProfileImage(),
                        fullImage ? ProfileImageType.FULL : ProfileImageType.COMPRESSED))
                .toList();
    }

    public UserModel getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public UserModel getUserBySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileImageResponse buildProfileImageResponse(
            Long userId,
            FileEntityModel image,
            ProfileImageType profileImageType) {
        boolean includeFullImage = profileImageType == ProfileImageType.FULL
                || profileImageType == ProfileImageType.BOTH;
        boolean includeCompressedImage = profileImageType == ProfileImageType.COMPRESSED
                || profileImageType == ProfileImageType.BOTH;

        return new UserProfileImageResponse(
                image.getId(),
                userId,
                image.getFileName(),
                image.getFileType(),
                includeFullImage ? image.getData() : null,
                includeCompressedImage ? image.getCompressedData() : null);
    }

    @Transactional(readOnly = true)
    public UserProfileImageResponse getProfileImage(HttpServletRequest request, boolean fullImage) {
        UserModel user = getUserBySession(request);
        FileEntityModel image = user.getProfileImage();
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not found");
        }
        return buildProfileImageResponse(user.getId(), image,
                fullImage ? ProfileImageType.FULL : ProfileImageType.COMPRESSED);
    }

    public UserProfileImageResponse updateProfileImage(MultipartFile profileImage, HttpServletRequest request) {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new RuntimeException("No profile image provided");
        }
        UserModel user = getUserBySession(request);
        FileEntityModel savedImage;
        long maxFileSizeKb = 50;
        long compressedMaxFileSizeKb = 5;
        if (user.getProfileImage() == null) {
            savedImage = storageService.storeImage(profileImage, maxFileSizeKb, compressedMaxFileSizeKb);
            user.setProfileImage(savedImage);
        } else {
            savedImage = storageService.updateImage(
                    user.getProfileImage().getId(),
                    profileImage,
                    maxFileSizeKb,
                    compressedMaxFileSizeKb);
        }
        userRepository.save(user);
        return buildProfileImageResponse(user.getId(), savedImage, ProfileImageType.BOTH);
    }

    @Transactional
    public void deleteProfileImage(HttpServletRequest request) {
        UserModel user = getUserBySession(request);
        FileEntityModel existingImage = user.getProfileImage();

        if (existingImage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile image not found");
        }

        user.setProfileImage(null);
        userRepository.save(user);
        storageService.deleteFile(existingImage.getId());
    }

    @Transactional
    public UserModel updateProfileBasics(ProfileBasicsUpdateRequest updatedDetails, HttpServletRequest request) {
        UserModel existingUser = getUserBySession(request);

        if (updatedDetails.getName() != null && !updatedDetails.getName().trim().isEmpty()) {
            existingUser.setName(updatedDetails.getName());
        }

        if (updatedDetails.getBio() != null) {
            existingUser.setBio(updatedDetails.getBio());
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public UserModel updateCredentials(ProfileCredentialsUpdateRequest updatedDetails, HttpServletRequest request) {
        UserModel existingUser = getUserBySession(request);

        validateCurrentPassword(updatedDetails.getCurrentPassword(), existingUser);

        if (updatedDetails.getUsername() != null && !updatedDetails.getUsername().trim().isEmpty()) {
            String normalizedUsername = updatedDetails.getUsername().trim();
            existingUser.setUsername(normalizedUsername);
        }

        if (updatedDetails.getPassword() != null && !updatedDetails.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedDetails.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    private void validatePassword(String rawPassword, UserModel user) {
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }
    }

    private void validateCurrentPassword(String rawPassword, UserModel user) {
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
    }

}
