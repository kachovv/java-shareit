package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
public class ItemRequestControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ItemRequestService itemRequestService;

    @Test
    void shouldCreateRequest() throws Exception {
        Long userId = 1L;
        ItemRequestDto request = new ItemRequestDto();
        request.setDescription("Мне нужна мощная дрель");

        ItemRequestDto response = new ItemRequestDto();
        response.setId(10L);
        response.setDescription(request.getDescription());

        when(itemRequestService.create(eq(userId), any(ItemRequestDto.class))).thenReturn(response);

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/requests")
                .header("X-Sharer-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.description").value("Мне нужна мощная дрель"));
        verify(itemRequestService).create(eq(userId), any(ItemRequestDto.class));
    }

    @Test
    void shouldReturnUserRequests() throws Exception {
        Long userId =1L;
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(10L);
        dto.setDescription("Мне нужна мощная дрель");

        ItemRequestDto dto2 = new ItemRequestDto();
        dto2.setId(11L);
        dto2.setDescription("Нужна лампа для ногтей");

        List<ItemRequestDto> responseList = List.of(dto, dto2);

        when(itemRequestService.getUserRequests(eq(userId))).thenReturn(responseList);

        mockMvc.perform(get("/requests")
                .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].description").value("Нужна лампа для ногтей"));
        verify(itemRequestService).getUserRequests(eq(userId));
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        Long userId = 1L;

        when(itemRequestService.getUserRequests(eq(userId))).thenReturn(List.of());

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(itemRequestService).getUserRequests(eq(userId));
    }

    @Test
    void shouldReturnAllOtherRequests() throws Exception {
        Long userId = 1L;
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(10L);
        dto.setDescription("Мне нужна мощная дрель");

        ItemRequestDto dto2 = new ItemRequestDto();
        dto2.setId(11L);
        dto2.setDescription("Нужна лампа для ногтей");

        List<ItemRequestDto> responseList = List.of(dto, dto2);

        when(itemRequestService.getAllOtherRequests(eq(userId), eq(0), eq(10))).thenReturn(responseList);

        mockMvc.perform(get("/requests/all")
                .header("X-Sharer-User-Id", userId)
                .param("from", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].description").value("Нужна лампа для ногтей"));
        verify(itemRequestService).getAllOtherRequests(eq(userId), eq(0), eq(10));
    }

    @Test
    void shouldUseDefaultPaginationValues_whenNotProvided() throws Exception {
        Long userId = 1L;
        when(itemRequestService.getAllOtherRequests(eq(userId), eq(0), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/requests/all")
                .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(itemRequestService).getAllOtherRequests(eq(userId), eq(0), eq(10));
    }

    @Test
    void shouldReturnRequestById() throws Exception {
        Long userId = 1L;
        Long itemRequestId = 10L;
        ItemRequestDto responseDto = new ItemRequestDto();
        responseDto.setId(itemRequestId);
        responseDto.setDescription("Нужна летающая метла");

        when(itemRequestService.getRequestById(eq(userId), eq(itemRequestId))).thenReturn(responseDto);

        mockMvc.perform(get("/requests/" + itemRequestId)
                .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Нужна летающая метла"));
        verify(itemRequestService).getRequestById(eq(userId), eq(itemRequestId));
    }
}
