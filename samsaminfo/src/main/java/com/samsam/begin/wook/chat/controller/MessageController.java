package com.samsam.begin.wook.chat.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.samsam.begin.wook.chat.dto.ChatRoomDTO;
import com.samsam.begin.wook.chat.dto.MessageDTO;
import com.samsam.begin.wook.chat.entity.CHATROOM;
import com.samsam.begin.wook.chat.service.ChatRoomService;
import com.samsam.begin.wook.chat.service.MessagesService;

@RestController
public class MessageController {

    private final SimpMessagingTemplate template;
    private final ChatRoomService chatRoomService;
    private final MessagesService messagesService;

    @Autowired
    public MessageController(SimpMessagingTemplate template, ChatRoomService chatRoomService, MessagesService messagesService) {
        this.template = template;
        this.chatRoomService = chatRoomService;
        this.messagesService = messagesService;
    }

    @PostMapping("/enter")
    public ResponseEntity<ChatRoomDTO> startChat(@RequestBody ChatRoomDTO chatRoomDTO) {
        String sellerId = chatRoomDTO.getSellerId();
        String buyerId = chatRoomDTO.getBuyerId();
        String roomTitle = chatRoomDTO.getRoomTitle();
        CHATROOM createdChatRoom = chatRoomService.createChatRoom(sellerId, buyerId, roomTitle);

        ChatRoomDTO createdChatRoomDTO = new ChatRoomDTO(
                createdChatRoom.getRoomId(),
                createdChatRoom.getSellerId(),
                createdChatRoom.getBuyerId(),
                createdChatRoom.getRoomTitle(),
                createdChatRoom.getCreatedAt()
        );

        return new ResponseEntity<>(createdChatRoomDTO, HttpStatus.CREATED);
    }

    @GetMapping("/enter/{roomId}")
    public ResponseEntity<ChatRoomDTO> enterChatRoom(@PathVariable("roomId") Long roomId) {
        CHATROOM chatRoom = chatRoomService.getChatRoom(roomId);
        if (chatRoom != null) {
            ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getRoomId(),
                chatRoom.getSellerId(),
                chatRoom.getBuyerId(),
                chatRoom.getRoomTitle(),
                chatRoom.getCreatedAt()
            );
            return new ResponseEntity<>(chatRoomDTO, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@Payload MessageDTO chatMessage, @DestinationVariable("roomId") Long roomId) {
        // 메시지를 저장
        messagesService.saveMessage(chatMessage);

        // 메시지를 특정 채널로 전송
        template.convertAndSend(String.format("/sub/chat/%s", roomId), chatMessage);
    }

    @GetMapping("/messages/{roomId}")
    public ResponseEntity<List<MessageDTO>> getMessagesByRoomId(@PathVariable("roomId") Long roomId) {
        List<MessageDTO> messages = messagesService.getMessagesByRoomId(roomId);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/chat-rooms")
    public ResponseEntity<List<ChatRoomDTO>> getChatRoomsByUserId(@RequestParam("userId") String userId) {
        List<CHATROOM> chatRooms = chatRoomService.getChatRoomsByUserId(userId);
        List<ChatRoomDTO> chatRoomDTOs = chatRooms.stream()
                .map(chatRoom -> new ChatRoomDTO(
                        chatRoom.getRoomId(),
                        chatRoom.getSellerId(),
                        chatRoom.getBuyerId(),
                        chatRoom.getRoomTitle(),
                        chatRoom.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(chatRoomDTOs);
    }
}
