package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class BookingServiceIntegrationTest {
    User owner;
    User booker;
    Item item;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Владелец");
        owner.setEmail("owner@mail.ru");
        userRepository.save(owner);

        booker = new User();
        booker.setName("Арендатор");
        booker.setEmail("booker@mail.ru");
        userRepository.save(booker);

        item = new Item();
        item.setName("Дрель");
        item.setDescription("Крутая");
        item.setOwner(owner);
        item.setAvailable(true);
        itemRepository.save(item);
    }

    @Test
    void shouldCreateBooking() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto createdBooking = bookingService.create(booker.getId(), dto);

        assertThat(createdBooking.getId()).isNotNull();
        assertThat(createdBooking.getStatus()).isEqualTo(BookingStatus.WAITING);

        Booking savedBooking = bookingRepository.findById(createdBooking.getId()).orElseThrow();
        assertThat(savedBooking.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void shouldThrowWhenBookerIsOwner() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(owner.getId(), dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldApproveBooking() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto createdBooking = bookingService.create(booker.getId(), dto);

        BookingDto approved = bookingService.approve(owner.getId(), createdBooking.getId(), true);
        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void shouldFindBookingById_getById() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto createdBooking = bookingService.create(booker.getId(), dto);

        BookingDto foundBooking = bookingService.getById(booker.getId(), createdBooking.getId());

        assertThat(foundBooking.getId()).isEqualTo(createdBooking.getId());
        assertThat(foundBooking.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void shouldThrowWhenRequestedByStranger_getById() {
        User stranger = new User();
        stranger.setName("Посторонний");
        stranger.setEmail("stranger@mail.ru");
        userRepository.save(stranger);

        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        BookingDto createdBooking = bookingService.create(booker.getId(), dto);

        Long strangerId = stranger.getId();
        Long bookingId = createdBooking.getId();

        assertThatThrownBy(() -> bookingService.getById(strangerId, bookingId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnAllBookingsByBooker() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));
        bookingService.create(booker.getId(), dto);

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Кувалда");
        itemDto.setDescription("Мощная");
        itemDto.setAvailable(true);
        ItemDto createdItem = itemService.create(owner.getId(), itemDto, null);

        BookingDto dto2 = new BookingDto();
        dto2.setItemId(createdItem.getId());
        dto2.setStart(LocalDateTime.now().plusDays(3));
        dto2.setEnd(LocalDateTime.now().plusDays(5));
        bookingService.create(booker.getId(), dto2);

        List<BookingDto> list = bookingService.getAllByBooker(
                booker.getId(),
                BookingState.ALL,
                0,
                10
        );
        assertThat(list).hasSize(2);
    }

    @Test
    void shouldReturnBookingsForAllStatesByBookerAndOwner() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        bookingService.create(booker.getId(), dto);

        for (BookingState state : BookingState.values()) {
            List<BookingDto> bookerBookings = bookingService.getAllByBooker(
                    booker.getId(), state, 0, 10
            );

            List<BookingDto> ownerBookings = bookingService.getAllByOwner(
                    owner.getId(), state, 0, 10
            );

            assertThat(bookerBookings).isNotNull();
            assertThat(ownerBookings).isNotNull();
        }
    }

    @Test
    void shouldThrowWhenItemIdIsNull() {
        BookingDto dto = new BookingDto();
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenItemIsUnavailable() {
        item.setAvailable(false);
        itemRepository.save(item);

        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(Long.MAX_VALUE, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowWhenItemDoesNotExist() {
        BookingDto dto = new BookingDto();
        dto.setItemId(Long.MAX_VALUE);
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowWhenBookingDatesAreNull() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setEnd(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);

        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(null);

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenStartDateIsAfterEndDate() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());

        LocalDateTime date = LocalDateTime.now().plusDays(2);
        dto.setStart(date);
        dto.setEnd(date);

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenStartDateIsInPast() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().minusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectBooking() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        BookingDto rejected = bookingService.approve(
                owner.getId(), created.getId(), false
        );

        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void shouldThrowWhenBookingApprovedByNonOwner() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        assertThatThrownBy(() ->
                bookingService.approve(booker.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldThrowWhenBookingAlreadyProcessed() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        bookingService.approve(owner.getId(), created.getId(), true);

        assertThatThrownBy(() ->
                bookingService.approve(owner.getId(), created.getId(), false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldReturnBookingForOwner() {
        BookingDto dto = new BookingDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusDays(1));
        dto.setEnd(LocalDateTime.now().plusDays(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        BookingDto found = bookingService.getById(owner.getId(), created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }
}
