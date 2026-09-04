package com.applab.applab_backend.common.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.service.ChatRoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalChatRoomInitializer implements CommandLineRunner {

    private final ChatRoomService chatRoomService;

    @Override
    public void run(String... args) {

        // This initializer runs automatically when the Spring Boot application starts.
        // It ensures that the GLOBAL chat room exists.
        ChatRoomRequest globalChatRoom = new ChatRoomRequest();
        globalChatRoom.setName("Global Chat");
        globalChatRoom.setRoomType(RoomType.GLOBAL);

        // The database may be temporarily unavailable during application startup
        // (for example, MySQL may be restarting because of server maintenance).
        // Retry the initialization instead of failing the entire application startup.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {
                // createChatRoom should be idempotent:
                // if the GLOBAL room already exists, it should not create a duplicate.
                chatRoomService.createChatRoom(globalChatRoom);

                log.info("Global chat room initialized successfully.");

                // Initialization succeeded, so no more retries are required.
                return;

            } catch (Exception e) {

                log.error(
                        "Global chat room initialization failed. Attempt {}/{}",
                        attempt,
                        maxAttempts,
                        e);

                // Wait before retrying to give the database time to become available.
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException interruptedException) {

                        // Restore the interrupted status instead of silently
                        // swallowing the interruption.
                        Thread.currentThread().interrupt();

                        log.warn(
                                "Global chat room initialization retry was interrupted.");

                        return;
                    }
                }
            }
        }

        // This initialization is not critical enough to prevent the whole backend
        // from starting. After all retries fail, log the problem and allow
        // Spring Boot to continue running.
        log.error(
                "Global chat room initialization failed after {} attempts. "
                        + "Application will continue running.",
                maxAttempts);
    }
}