package ru.practicum.shareit.item.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    public ItemServiceImpl(ItemRepository itemRepository, UserRepository userRepository, BookingRepository bookingRepository, CommentRepository commentRepository, ItemRequestRepository itemRequestRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.commentRepository = commentRepository;
        this.itemRequestRepository = itemRequestRepository;
    }

    @Transactional
    @Override
    public ItemDto create(Long userId, ItemDto itemDto, Long requestId) {
        validateItemDto(itemDto);
        User owner = getUserOrThrow(userId);

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        ItemRequest request = null;
        if (requestId != null) {
            request = itemRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не существует"));
        }
        item.setRequest(request);

        Item saved = itemRepository.save(item);
        log.info("Добавлена вещь с id = {} владельцем id = {}", saved.getId(), userId);
        return ItemMapper.toItemDto(saved);
    }

    @Transactional
    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        Item item = getItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь с id = " + itemId + " не найдена у пользователя с id = " + userId);
        }
        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        Item saved = itemRepository.save(item);
        log.info("Обновлена вещи с id = {}", itemId);

        return ItemMapper.toItemDto(saved);
    }

    @Transactional
    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentDto dto) {
        if (dto.getText() == null || dto.getText().isBlank()) {
            throw new ValidationException("Текст комментария не может быть пустым");
        }
        User author = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        boolean hasRented = bookingRepository.existsByBooker_IdAndItem_IdAndEndBeforeAndStatus(userId, itemId, LocalDateTime.now(), BookingStatus.APPROVED);
        if (!hasRented) {
            throw new ValidationException("Пользователь не брал вещь в аренду");
        }
        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        log.info("Добавлен комментарий автором с id = {} на вещь с id = {}", author.getId(), item.getId());
        return CommentMapper.toCommentDto(saved);
    }

    @Override
    public ItemWithBookingsDto getById(Long userId, Long itemId) {
        Item item = getItemOrThrow(itemId);
        ItemWithBookingsDto dto = ItemMapper.toItemWithBookingsDto(item);

        List<Comment> comments = commentRepository.findAllByItem_Id(itemId);
        dto.setComments(comments.stream().map(CommentMapper::toCommentDto).toList());

        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            dto.setLastBooking(findLastBooking(itemId, now));
            dto.setNextBooking(findNextBooking(itemId, now));
        }
        return dto;
    }

    @Override
    public List<ItemWithBookingsDto> getAllByOwner(Long userId) {
        List<Item> items = itemRepository.findAllByOwnerId(userId);
        LocalDateTime now = LocalDateTime.now();

        return items.stream().map(item -> {
            ItemWithBookingsDto dto = ItemMapper.toItemWithBookingsDto(item);

            List<Comment> comments = commentRepository.findAllByItem_Id(item.getId());
            dto.setComments(comments.stream().map(CommentMapper::toCommentDto).toList());
            dto.setLastBooking(findLastBooking(item.getId(), now));
            dto.setNextBooking(findNextBooking(item.getId(), now));

            return dto;
        }).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private void validateItemDto(ItemDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new ValidationException("Описание не может быть пустым");
        }
        if (dto.getAvailable() == null) {
            throw new ValidationException("Статус доступности должен быть указан");
        }
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id = " + itemId + " не найдена"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    private BookingShortDto findLastBooking(Long itemId, LocalDateTime now) {
        List<Booking> bookings = bookingRepository.findAllByItem_Id(itemId);

        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getEnd().isBefore(now))
                .max(Comparator.comparing(Booking::getStart))
                .map(this::toBookingShortDto)
                .orElse(null);
    }

    private BookingShortDto findNextBooking(Long itemId, LocalDateTime now) {
        List<Booking> bookings = bookingRepository.findAllByItem_Id(itemId);

        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(this::toBookingShortDto)
                .orElse(null);
    }

    private BookingShortDto toBookingShortDto(Booking booking) {
        return new BookingShortDto(
                booking.getId(),
                booking.getBooker().getId(),
                booking.getStart(),
                booking.getEnd()
        );
    }
}