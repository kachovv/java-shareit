package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.controller.BookingController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
public class BookingControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BookingService bookingService;

    @Test
    void shouldCreateBooking() throws Exception {
        Long userId = 1L;
        BookingDto request = new BookingDto();
        request.setItemId(10L);
        request.setStart(LocalDateTime.now().plusDays(1));
        request.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto response = new BookingDto();
        response.setId(1L);
        response.setItemId(10L);
        response.setStart(request.getStart());
        response.setEnd(request.getEnd());
        response.setStatus(BookingStatus.WAITING);

        when(bookingService.create(eq(userId), any(BookingDto.class))).thenReturn(response);

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));
        verify(bookingService).create(eq(userId), any(BookingDto.class));
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsException() throws Exception {
        Long userId = 1L;
        BookingDto dto = new BookingDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        String json = objectMapper.writeValueAsString(dto);

        when(bookingService.create(eq(userId), any(BookingDto.class)))
                .thenThrow(new ValidationException("Дата начала не может быть в прошлом"));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(bookingService).create(eq(userId), any(BookingDto.class));
    }

    @Test
    void shouldApproveBooking() throws Exception {
        Long userId = 1L;
        Long bookingId = 1L;
        boolean approved = true;

        BookingDto response = new BookingDto();
        response.setId(bookingId);
        response.setItemId(10L);
        response.setStart(LocalDateTime.now().plusDays(1));
        response.setEnd(LocalDateTime.now().plusDays(2));
        response.setStatus(BookingStatus.APPROVED);

        when(bookingService.approve(eq(userId), eq(bookingId), eq(approved))).thenReturn(response);

        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", userId)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        verify(bookingService).approve(eq(userId), eq(bookingId), eq(approved));
    }

    @Test
    void shouldReturnBookingById() throws Exception {
        Long userId = 1L;
        Long bookingId = 10L;
        BookingDto response = new BookingDto();
        response.setId(bookingId);
        response.setItemId(10L);
        response.setStart(LocalDateTime.now().plusDays(1));
        response.setEnd(LocalDateTime.now().plusDays(2));
        response.setStatus(BookingStatus.APPROVED);

        when(bookingService.getById(eq(userId), eq(bookingId))).thenReturn(response);

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
        verify(bookingService).getById(eq(userId), eq(bookingId));
    }

    @Test
    void shouldReturnAllBookingsByOwner() throws Exception {
        Long userId = 1L;
        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(1L);
        bookingDto.setStatus(BookingStatus.APPROVED);
        List<BookingDto> responseList = List.of(bookingDto);

        when(bookingService.getAllByOwner(eq(userId), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(responseList);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", userId)
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
        verify(bookingService).getAllByOwner(eq(userId), eq(BookingState.ALL), eq(0), eq(10));
    }

    @Test
    void shouldUseDefaultPaginationValues_whenNotProvided() throws Exception {
        Long userId = 1L;
        when(bookingService.getAllByOwner(eq(userId), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(List.of());
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", userId)
                        .param("state", "ALL"))
                .andExpect(status().isOk());
        verify(bookingService).getAllByOwner(eq(userId), eq(BookingState.ALL), eq(0), eq(10));
    }

    @Test
    void shouldReturnAllBookingsByBooker() throws Exception {
        Long userId = 1L;
        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(1L);
        bookingDto.setStatus(BookingStatus.APPROVED);
        List<BookingDto> responseList = List.of(bookingDto);

        when(bookingService.getAllByBooker(eq(userId), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(responseList);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", userId)
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
        verify(bookingService).getAllByBooker(eq(userId), eq(BookingState.ALL), eq(0), eq(10));
    }
}
