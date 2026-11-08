package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto create(Long userId, ItemRequestDto dto);

    List<ItemRequestDto> getUserRequests(Long userId);

    List<ItemRequestDto> getAllOtherRequests(Long userId, int from, int size);

    ItemRequestDto getRequestById(Long userId, Long requestId);
}
