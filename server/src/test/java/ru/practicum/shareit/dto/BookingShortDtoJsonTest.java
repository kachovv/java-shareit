package ru.practicum.shareit.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class BookingShortDtoJsonTest {
    @Autowired
    private JacksonTester<BookingShortDto> jacksonTester;

    @Test
    void shouldSerializeBookingShortDto() throws Exception {
        BookingShortDto dto = new BookingShortDto();
        dto.setId(1L);
        dto.setBookerId(10L);
        dto.setStart(LocalDateTime.of(2024, 12, 1, 10, 0));
        dto.setEnd(LocalDateTime.of(2024, 12, 2, 12, 0));

        JsonContent<BookingShortDto> result = jacksonTester.write(dto);

        assertThat(result).hasJsonPathNumberValue("$.id", 1L);
        assertThat(result).hasJsonPathNumberValue("$.bookerId", 10L);
        assertThat(result).hasJsonPathStringValue("$.start", "2024-12-01T10:00:00");
    }

    @Test
    void shouldDeserializeBookingDto() throws Exception {
        String json = """
                {
                  "id": 1,
                  "bookerId": 10,
                  "start": "2024-12-01T10:00:00",
                  "end": "2024-12-02T12:00:00"
                }
                """;
        BookingShortDto result = jacksonTester.parseObject(json);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBookerId()).isEqualTo(10L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2024, 12, 1, 10, 0));
    }
}
