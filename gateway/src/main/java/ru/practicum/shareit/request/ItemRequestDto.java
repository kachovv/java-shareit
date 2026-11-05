package ru.practicum.shareit.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDto {
    private Long id;

    @NotBlank(message = "Описание не может быть пустым")
    private String description;

    private LocalDateTime created;
}