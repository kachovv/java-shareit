package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.controller.UserController;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void shouldCreateUser() throws Exception {
        UserDto requestDto = new UserDto(null, "Иван", "ivan@mail.ru");
        UserDto responseDto = new UserDto(1L, "Иван", "ivan@mail.ru");

        when(userService.createUser(any(UserDto.class))).thenReturn(responseDto);
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"));

        verify(userService).createUser(any(UserDto.class));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        Long userId = 1L;
        UserDto requestDto = new UserDto(null, "Новое Имя", null);
        UserDto responseDto = new UserDto(userId, "Новое Имя", "old@mail.ru");

        when(userService.updateUser(eq(userId), any(UserDto.class))).thenReturn(responseDto);

        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch("/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое Имя"));
        verify(userService).updateUser(eq(userId), any(UserDto.class));
    }

    @Test
    void shouldReturnNotFound_whenUpdatingNonExistentUser() throws Exception {
        Long invalidId = 999L;
        UserDto requestDto = new UserDto(null, "Имя", "mail@mail.ru");
        String json = objectMapper.writeValueAsString(requestDto);

        when(userService.updateUser(eq(invalidId), any(UserDto.class)))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(patch("/users/" + invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());

        verify(userService).updateUser(eq(invalidId), any(UserDto.class));
    }

    @Test
    void shouldReturnUser() throws Exception {
        Long userId = 1L;
        UserDto responseDto = new UserDto(1L, "Иван", "ivan@mail.ru");

        when(userService.getById(eq(userId))).thenReturn(responseDto);

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));

        verify(userService).getById((eq(userId)));
    }

    @Test
    void shouldReturnNotFount_whenUserDoesNotExist() throws Exception {
        Long invalidId = 999L;
        when(userService.getById(eq(invalidId))).thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/" + invalidId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllUsers() throws Exception {
        UserDto user1 = new UserDto(1L, "Иван", "ivan@mail.ru");
        UserDto user2 = new UserDto(2L, "Петр", "petr@mail.ru");
        List<UserDto> responseList = List.of(user1, user2);

        when(userService.getAll()).thenReturn(responseList);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Иван"))
                .andExpect(jsonPath("$[1].name").value("Петр"));
        verify(userService).getAll();
    }

    @Test
    void shouldReturnEmptyList_whenNoUsers() throws Exception {
        when(userService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(userService).getAll();
    }

    @Test
    void shouldDeleteUser() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isOk());
        verify(userService).delete(eq(userId));
    }
}
