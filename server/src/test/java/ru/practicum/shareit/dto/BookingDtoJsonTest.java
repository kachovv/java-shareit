package ru.practicum.shareit.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class BookingDtoJsonTest {
    @Autowired
    private JacksonTester<BookingDto> jacksonTester;

    @Test
    void shouldSerializeBookingDto() throws Exception {
        BookingDto dto = new BookingDto();
        dto.setId(1L);
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.of(2024, 12, 1, 10, 0));
        dto.setEnd(LocalDateTime.of(2024, 12, 2, 12, 0));
        dto.setStatus(BookingStatus.WAITING);

        JsonContent<BookingDto> result = jacksonTester.write(dto);

        assertThat(result).hasJsonPathNumberValue("$.id", 1L);
        assertThat(result).hasJsonPathNumberValue("$.itemId", 10L);
        assertThat(result).hasJsonPathStringValue("$.start", "2024-12-01T10:00:00");
        assertThat(result).hasJsonPathStringValue("$.status", "WAITING");
    }

    @Test
    void shouldDeserializeBookingDto() throws Exception {
        String json = "{\"id\":1, \"itemId\":10, \"start\":\"2024-12-01T10:00:00\", \"end\":\"2024-12-02T12:00:00\", \"status\":\"WAITING\"}";
        BookingDto result = jacksonTester.parseObject(json);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getItemId()).isEqualTo(10L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2024, 12, 1, 10, 0));
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
    }
}
