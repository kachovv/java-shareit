package ru.practicum.shareit.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public ItemRequestServiceImpl(ItemRequestRepository itemRequestRepository, UserRepository userRepository, ItemRepository itemRepository) {
        this.itemRequestRepository = itemRequestRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto dto) {
        validateItemRequestDto(dto);
        User requester = getUserOrThrow(userId);

        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now());

        ItemRequest saved = itemRequestRepository.save(request);

        log.info("Добавлен запрос с id = {} и владельцем id = {}", saved.getId(), requester.getId());
        return ItemRequestMapper.toItemRequestDto(saved, List.of());
    }

    @Override
    public List<ItemRequestDto> getUserRequests(Long userId) {
        getUserOrThrow(userId);
        List <ItemRequest> requests = itemRequestRepository.findAllByRequester_Id(userId, Sort.by(Sort.Direction.DESC, "created"));

        return requests.stream().map(request -> {
            List<Item> items = itemRepository.findAllByRequest_Id(request.getId());
            return ItemRequestMapper.toItemRequestDto(request, items);
        }).toList();
    }

    @Override
    public List<ItemRequestDto> getAllOtherRequests(Long userId, int from, int size) {
        getUserOrThrow(userId);
        if (from < 0 || size <= 0) {
            throw new ValidationException("Параметры пагинации 'from' и 'size' должны быть положительными");
        }
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        List <ItemRequest> requests = itemRequestRepository.findAllByRequester_IdNot(userId, pageable);
        return requests.stream().map(request -> {
            List<Item> items = itemRepository.findAllByRequest_Id(request.getId());
            return ItemRequestMapper.toItemRequestDto(request, items);
        }).toList();
    }

    @Override
    public ItemRequestDto getRequestById(Long userId, Long requestId) {
        getUserOrThrow(userId);
        ItemRequest itemRequest = getItemRequestOrThrow(requestId);
        List<Item> items = itemRepository.findAllByRequest_Id(requestId);
        return ItemRequestMapper.toItemRequestDto(itemRequest, items);
    }

    private void validateItemRequestDto(ItemRequestDto dto) {
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new ValidationException("Описание запроса не может быть пустым");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    private ItemRequest getItemRequestOrThrow(Long requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден"));
    }
}