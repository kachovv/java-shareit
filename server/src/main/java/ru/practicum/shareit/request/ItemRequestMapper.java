package ru.practicum.shareit.request;

import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public class ItemRequestMapper {
    public static ItemRequestDto toItemRequestDto(ItemRequest itemRequest, List<Item> items) {
        List<ItemShortDto> itemDtos = items.stream()
                .map(item -> new ItemShortDto(
                        item.getId(),
                        item.getName(),
                        item.getOwner().getId()
                ))
                .toList();

        return new ItemRequestDto(
                itemRequest.getId(),
                itemRequest.getDescription(),
                itemRequest.getCreated(),
                itemDtos
        );
    }
}
