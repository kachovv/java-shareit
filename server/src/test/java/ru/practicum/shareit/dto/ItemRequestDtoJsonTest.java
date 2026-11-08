package ru.practicum.shareit.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class ItemRequestDtoJsonTest {
    @Autowired
    private JacksonTester<ItemRequestDto> jacksonTester;

    @Test
    void shouldSerializeItemRequestDto() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(1L);
        dto.setDescription("Нужна новая дрель");
        dto.setCreated(LocalDateTime.of(2024, 12, 1, 10, 0));

        JsonContent<ItemRequestDto> result = jacksonTester.write(dto);

        assertThat(result).hasJsonPathNumberValue("$.id", 1L);
        assertThat(result).hasJsonPathStringValue("$.description", "Нужна новая дрель");
        assertThat(result).hasJsonPathStringValue("$.created", "2024-12-01T10:00:00");
    }

    @Test
    void shouldDeserializeItemRequestDto() throws Exception {
        String json = "{"
                + "\"id\": 1,"
                + "\"description\": \"Нужна новая дрель\","
                + "\"created\": \"2024-12-01T10:00:00\""
                + "}";
        ItemRequestDto result = jacksonTester.parseObject(json);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Нужна новая дрель");
        assertThat(result.getCreated()).isEqualTo(LocalDateTime.of(2024, 12, 1, 10, 0));
    }
}
