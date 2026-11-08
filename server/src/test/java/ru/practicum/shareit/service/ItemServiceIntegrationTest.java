package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
public class ItemServiceIntegrationTest {
    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User owner;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Пользователь");
        owner.setEmail("owner@mail.ru");
        userRepository.save(owner);

        item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная");
        item.setAvailable(true);
        item.setOwner(owner);
        itemRepository.save(item);
    }

    @Test
    void shouldCreateItem() {
        ItemDto newItemDto = new ItemDto(null, "Лопата", "Штыковая", true);
        ItemDto createdItem = itemService.create(owner.getId(), newItemDto, null);
        assertThat(createdItem.getId()).isNotNull();
        assertThat(createdItem.getName()).isEqualTo("Лопата");

        Item savedItem = itemRepository.findById(createdItem.getId()).orElseThrow();
        assertThat(savedItem.getName()).isEqualTo("Лопата");
        assertThat(savedItem.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void shouldThrowWhenItemNameIsBlank() {
        ItemDto dto = new ItemDto(null, " ", "Описание", true);

        assertThatThrownBy(() -> itemService.create(owner.getId(), dto, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenItemDescriptionIsBlank() {
        ItemDto dto = new ItemDto(null, "Дрель", " ", true);

        assertThatThrownBy(() -> itemService.create(owner.getId(), dto, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldThrowWhenItemAvailabilityIsNull() {
        ItemDto dto = new ItemDto(null, "Дрель", "Описание", null);

        assertThatThrownBy(() -> itemService.create(owner.getId(), dto, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldUpdateItem() {
        ItemDto newItemDto = new ItemDto(null, "Новая дрель", "Очень мощная", false);
        ItemDto updatedItem = itemService.update(owner.getId(), item.getId(), newItemDto);
        assertThat(updatedItem.getName()).isEqualTo("Новая дрель");
        assertThat(updatedItem.getAvailable()).isFalse();

        Item itemFromDb = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(itemFromDb.getName()).isEqualTo("Новая дрель");
    }

    @Test
    void shouldFindItem_search() {
        List<ItemDto> list = itemService.search("дрель");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Дрель");
    }

    @Test
    void shouldReturnEmptyList_search() {
        List<ItemDto> list = itemService.search("Несуществующий");
        assertThat(list).isEmpty();

        item.setAvailable(false);
        itemRepository.save(item);
        List<ItemDto> list2 = itemService.search("Дрель");
        assertThat(list2).isEmpty();
    }

    @Test
    void shouldReturnItemById_getById() {
        ItemWithBookingsDto itemDto = itemService.getById(owner.getId(), item.getId());
        assertThat(itemDto.getId()).isEqualTo(item.getId());
        assertThat(itemDto.getName()).isEqualTo("Дрель");
    }

    @Test
    void shouldThrow_getById() {
        assertThatThrownBy(() -> itemService.getById(owner.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnAllItemsByOwner_getAllByOwner() {
        Item item2 = new Item();
        item2.setName("Молоток");
        item2.setDescription("Компактный");
        item2.setAvailable(true);
        item2.setOwner(owner);
        itemRepository.save(item2);

        Item item3 = new Item();
        item3.setName("Ваза");
        item3.setDescription("Для цветов");
        item3.setAvailable(true);
        item3.setOwner(owner);
        itemRepository.save(item3);

        Booking booking = new Booking();
        booking.setStart(LocalDateTime.now().minusDays(3));
        booking.setEnd(LocalDateTime.now().minusDays(2));
        booking.setItem(item);
        booking.setBooker(owner);
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        Booking booking2 = new Booking();
        booking2.setStart(LocalDateTime.now().plusDays(2));
        booking2.setEnd(LocalDateTime.now().plusDays(3));
        booking2.setItem(item);
        booking2.setBooker(owner);
        booking2.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking2);

        Comment comment = new Comment();
        comment.setText("Крутая вещь");
        comment.setItem(item);
        comment.setAuthor(owner);
        comment.setCreated(LocalDateTime.now());
        commentRepository.save(comment);


        List<ItemWithBookingsDto> items = itemService.getAllByOwner(owner.getId());

        assertThat(items).hasSize(3);

        ItemWithBookingsDto dto = items.stream()
                .filter(i -> i.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(dto.getLastBooking()).isNotNull();
        assertThat(dto.getNextBooking()).isNotNull();
        assertThat(dto.getComments()).hasSize(1);
    }
}
