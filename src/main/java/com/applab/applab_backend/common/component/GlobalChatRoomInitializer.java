package com.applab.applab_backend.common.component;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.applab.applab_backend.chatroom.dto.ChatRoomRequest;
import com.applab.applab_backend.chatroom.enums.RoomType;
import com.applab.applab_backend.chatroom.service.ChatRoomService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GlobalChatRoomInitializer implements CommandLineRunner {
    private final ChatRoomService chatRoomService;

    @Override
    public void run(String... args) {
        ChatRoomRequest globalChatRoom = new ChatRoomRequest();
        globalChatRoom.setName("Global Chat");
        globalChatRoom.setRoomType(RoomType.GLOBAL);
        chatRoomService.createChatRoom(globalChatRoom);
    }
}
