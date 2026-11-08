package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class ItemRequestServiceIntegrationTest {
    User requester;
    @Autowired
    private ItemRequestService itemRequestService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setName("Пользователь");
        requester.setEmail("user@mail.ru");
        userRepository.save(requester);
    }

    @Test
    void shouldCreateRequest() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setDescription("Нужна дрель");
        ItemRequestDto createdRequest = itemRequestService.create(requester.getId(), requestDto);

        assertThat(createdRequest.getId()).isNotNull();
        assertThat(createdRequest.getDescription()).isEqualTo("Нужна дрель");
        assertThat(createdRequest.getCreated()).isNotNull();
        assertThat(createdRequest.getItems()).isEmpty();

        ItemRequest savedRequest = itemRequestRepository.findById(createdRequest.getId()).orElseThrow();
        assertThat(savedRequest.getDescription()).isEqualTo("Нужна дрель");
        assertThat(savedRequest.getRequester().getId()).isEqualTo(requester.getId());
    }

    @Test
    void shouldThrowWhenDescriptionIsEmpty() {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setDescription("    ");
        assertThatThrownBy(() -> itemRequestService.create(requester.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldReturnUserRequests() {
        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setDescription("Нужна дрель");
        itemRequest.setRequester(requester);
        itemRequest.setCreated(LocalDateTime.now().minusDays(1));
        itemRequestRepository.save(itemRequest);

        ItemRequest itemRequest2 = new ItemRequest();
        itemRequest2.setDescription("Нужна лопата");
        itemRequest2.setRequester(requester);
        itemRequest2.setCreated(LocalDateTime.now());
        itemRequestRepository.save(itemRequest2);

        List<ItemRequestDto> requests = itemRequestService.getUserRequests(requester.getId());

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getDescription()).isEqualTo("Нужна лопата");
        assertThat(requests.get(1).getDescription()).isEqualTo("Нужна дрель");
    }

    @Test
    void shouldReturnAllOtherRequests() {
        User otherUser = new User();
        otherUser.setName("Другой Пользователь");
        otherUser.setEmail("otherUser@mail.ru");
        userRepository.save(otherUser);

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setDescription("Нужна дрель");
        itemRequest.setRequester(requester);
        itemRequest.setCreated(LocalDateTime.now().minusDays(1));
        itemRequestRepository.save(itemRequest);

        ItemRequest itemRequest2 = new ItemRequest();
        itemRequest2.setDescription("Нужна лопата");
        itemRequest2.setRequester(requester);
        itemRequest2.setCreated(LocalDateTime.now());
        itemRequestRepository.save(itemRequest2);

        ItemRequest itemRequest3 = new ItemRequest();
        itemRequest3.setDescription("Нужен фен дайсон");
        itemRequest3.setRequester(otherUser);
        itemRequest3.setCreated(LocalDateTime.now().minusDays(4));
        itemRequestRepository.save(itemRequest3);

        List<ItemRequestDto> result = itemRequestService.getAllOtherRequests(requester.getId(), 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Нужен фен дайсон");
    }

    @Test
    void shouldReturnPaginatedRequests() {
        User otherUser = new User();
        otherUser.setName("Другой");
        otherUser.setEmail("other@mail.ru");
        otherUser = userRepository.save(otherUser);

        for (int i = 1; i <= 3; i++) {
            ItemRequest req = new ItemRequest();
            req.setDescription("Запрос " + i);
            req.setRequester(otherUser);
            req.setCreated(LocalDateTime.now().minusMinutes(i));
            itemRequestRepository.save(req);
        }

        List<ItemRequestDto> page1 = itemRequestService.getAllOtherRequests(requester.getId(), 0, 2);

        List<ItemRequestDto> page2 = itemRequestService.getAllOtherRequests(requester.getId(), 2, 2);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
    }

    @Test
    void shouldReturnRequestById() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setDescription("Нужна дрель");
        ItemRequestDto createdRequest = itemRequestService.create(requester.getId(), requestDto);

        ItemRequestDto savedRequest = itemRequestService.getRequestById(requester.getId(), createdRequest.getId());

        assertThat(savedRequest.getId()).isEqualTo(createdRequest.getId());
        assertThat(savedRequest.getDescription()).isEqualTo(createdRequest.getDescription());
    }

    @Test
    void shouldThrowException_whenRequestNotFound() {
        assertThatThrownBy(() -> itemRequestService.getRequestById(requester.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }
}
