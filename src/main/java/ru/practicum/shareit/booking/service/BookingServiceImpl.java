package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public BookingDto create(Long userId, BookingDto dto) {
        if (dto.getItemId() == null) {
            throw new ValidationException("itemId не указан");
        }
        User booker = getUserOrThrow(userId);
        Item item = getItemOrThrow(dto.getItemId());

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для аренды");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }
        validateBookingDates(dto.getStart(), dto.getEnd());

        Booking booking = BookingMapper.toBooking(dto);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        Booking saved = bookingRepository.save(booking);
        log.info("Создано бронирование id = {} для вещи id = {} пользователем id = {}",
                saved.getId(), item.getId(), userId);
        return BookingMapper.toBookingDto(saved);
    }

    @Transactional
    @Override
    public BookingDto approve(Long userId, Long bookingId, Boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Подтверждать бронирования может только владелец вещи");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);

        Booking saved = bookingRepository.save(booking);
        log.info("Бронирование id = {} {}", bookingId, approved ? "подтверждено" : "отклонено");
        return BookingMapper.toBookingDto(saved);
    }

    @Transactional
    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Бронирование с id = " + bookingId + " не найдено");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByBooker(Long userId, BookingState state) {
        getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        List<Booking> bookings = switch (state) {
            case ALL      -> bookingRepository.findByBooker_Id(userId, sort);
            case WAITING  -> bookingRepository.findByBooker_IdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(userId, BookingStatus.REJECTED, sort);
            case PAST     -> bookingRepository.findByBooker_IdAndEndBefore(userId, now, sort);
            case FUTURE   -> bookingRepository.findByBooker_IdAndStartAfter(userId, now, sort);
            case CURRENT  -> bookingRepository.findCurrentByBooker(userId, now, sort);
        };
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, BookingState state) {
        getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        List<Booking> bookings = switch (state) {
            case ALL      -> bookingRepository.findByItem_Owner_Id(userId, sort);
            case WAITING  -> bookingRepository.findByItem_Owner_IdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByItem_Owner_IdAndStatus(userId, BookingStatus.REJECTED, sort);
            case PAST     -> bookingRepository.findByItem_Owner_IdAndEndBefore(userId, now, sort);
            case FUTURE   -> bookingRepository.findByItem_Owner_IdAndStartAfter(userId, now, sort);
            case CURRENT  -> bookingRepository.findCurrentByOwner(userId, now, sort);
        };
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Вещь с id = " + id + " не найдена"));
    }

    private Booking getBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id + " + id + " не найдено"));
    }

    private void validateBookingDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Даты бронирования не могут быть пустыми");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Дата начала не может быть в прошлом");
        }
    }
}