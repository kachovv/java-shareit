package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(ItemControllerTest.class)
public class ItemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void shouldCreateItem() throws Exception {
        Long userId = 1L;
        ItemDto requestDto = new ItemDto(null, "Дрель", "Мощная", true);
        ItemDto responseDto = new ItemDto(1L, "Дрель", "Мощная", true);

        when(itemService.create(eq(userId), any(ItemDto.class), any())).thenReturn(responseDto);
        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Дрель"));
        verify(itemService).create(eq(userId), any(ItemDto.class), any());
    }

    @Test
    void shouldReturnNotFoundWhenUserIdIsInvalid() throws Exception {
        Long invalidId = 999L;
        ItemDto requestDto = new ItemDto(null, "Дрель", "Мощная", true);
        String json = objectMapper.writeValueAsString(requestDto);

        when(itemService.create(eq(invalidId), any(ItemDto.class), any()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
        verify(itemService).create(eq(invalidId), any(ItemDto.class), any());
    }

    @Test
    void shouldUpdateItem() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;
        ItemDto requestDto = new ItemDto(null, "Дрель", null, null);
        ItemDto responseDto = new ItemDto(itemId, "Дрель", "Мощная", true);
        when(itemService.update(eq(userId), eq(itemId), any(ItemDto.class))).thenReturn(responseDto);

        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(patch("/items/" + itemId)
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Дрель"));
        verify(itemService).update(eq(userId), eq(itemId), any(ItemDto.class));
    }

    @Test
    void shouldReturnNotFoundWhenInvalidItemId() throws Exception {
        Long userId = 1L;
        Long invalidItemId = 999L;
        ItemDto requestDto = new ItemDto(null, "Дрель", null, null);
        String json = objectMapper.writeValueAsString(requestDto);

        when(itemService.update(eq(userId), eq(invalidItemId), any(ItemDto.class))).thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(patch("/items/" + invalidItemId)
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
        verify(itemService).update(eq(userId), eq(invalidItemId), any(ItemDto.class));
    }

    @Test
    void shouldReturnItemById() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;
        ItemWithBookingsDto responseDto = new ItemWithBookingsDto(
                itemId, "Дрель", "Мощная", true, null, null, List.of()
        );

        when(itemService.getById(eq(userId), eq(itemId))).thenReturn(responseDto);

        mockMvc.perform(get("/items/" + itemId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Дрель"));
        verify(itemService).getById(eq(userId), eq(itemId));
    }

    @Test
    void shouldReturnAllItems() throws Exception {
        Long userId = 1L;
        ItemWithBookingsDto itemDto1 = new ItemWithBookingsDto(
                1L, "Дрель", "Мощная", true, null, null, List.of()
        );
        ItemWithBookingsDto itemDto2 = new ItemWithBookingsDto(
                2L, "Пила", "Наточенная", false, null, null, List.of()
        );
        List<ItemWithBookingsDto> list = List.of(itemDto1, itemDto2);

        when(itemService.getAllByOwner(eq(userId))).thenReturn(list);

        mockMvc.perform(get("/items/" + userId)
                .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Дрель"))
                .andExpect(jsonPath("$[1].name").value("Пила"));
        verify(itemService).getAllByOwner(eq(userId));
    }

    @Test
    void shouldReturnEmptyList_whenNoItems() throws Exception {
        Long userId = 1L;
        when(itemService.getAllByOwner(userId)).thenReturn(List.of());

        mockMvc.perform(get("/items/" + userId)
                .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(itemService).getAllByOwner(eq(userId));
    }

    @Test
    void shouldSearchItems() throws Exception {
        String searchText = "дрель";
        ItemDto itemDto = new ItemDto(1L, "Дрель Салют", "Мощная", true);

        when(itemService.search(eq(searchText))).thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items/search")
                .param("text", searchText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length").value(1))
                .andExpect(jsonPath("$[0].name").value("Дрель Салют"));
        verify(itemService).search(eq(searchText));
    }

    @Test
    void shouldAddComment() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;
        CommentDto requestDto = new CommentDto(null, "Отличная дрель!", null, null);
        CommentDto responseDto = new CommentDto(1L, "Отличная дрель!", "Иван", LocalDateTime.now());

        when(itemService.addComment(eq(userId), eq(itemId), any(CommentDto.class)))
                .thenReturn(responseDto);

        String json = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                .header("X-Sharer-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.text").value("Отличная дрель!"))
                .andExpect(jsonPath("$.authorName").value("Иван"));
        verify(itemService).addComment(eq(userId), eq(itemId), any(CommentDto.class));
    }

    @Test
    void shouldReturnBadRequest_whenCommentTextIsBlank() throws Exception {
        Long userId = 1L;
        Long itemId = 1L;
        CommentDto invalidDto = new CommentDto(null, "", null, null);
        String json = objectMapper.writeValueAsString(invalidDto);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                .header("X-Sharer-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
        verify(itemService, never()).addComment(any(), any(), any());
    }
}
