package ru.practicum.shareit.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateUser() {
        UserDto userDto = new UserDto(null, "Пользователь", "owner@mail.ru");
        UserDto createdUser = userService.createUser(userDto);

        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("Пользователь");

        User savedUserInDb = userRepository.findById(createdUser.getId()).orElseThrow();
        assertThat(savedUserInDb.getEmail()).isEqualTo("owner@mail.ru");
    }

    @Test
    void shouldThrowWhenEmailIsDuplicate_createUser() {
        userService.createUser(new UserDto(null, "Иван", "ivan@mail.ru"));
        UserDto duplicateEmailUser = new UserDto(null, "Петр", "ivan@mail.ru");
        assertThatThrownBy(() -> userService.createUser(duplicateEmailUser))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldUpdateUser() {
        UserDto userDto = new UserDto(null, "Пользователь", "owner@mail.ru");
        UserDto createdUser = userService.createUser(userDto);
        UserDto newUserDto = new UserDto(null, "Обновленное имя", "newOwner@mail.ru");
        UserDto updatedUser = userService.updateUser(createdUser.getId(), newUserDto);

        assertThat(updatedUser.getName()).isEqualTo("Обновленное имя");
        assertThat(updatedUser.getEmail()).isEqualTo("newOwner@mail.ru");
    }

    @Test
    void shouldDeleteUser() {
        UserDto userDto = new UserDto(null, "Пользователь", "owner@mail.ru");
        UserDto createdUser = userService.createUser(userDto);
        userService.delete(createdUser.getId());
        assertThat(userRepository.findById(createdUser.getId())).isEmpty();
    }

    @Test
    void shouldReturnUserById_getById() {
        UserDto userDto = new UserDto(null, "Пользователь", "owner@mail.ru");
        UserDto createdUser = userService.createUser(userDto);
        UserDto userFound = userService.getById(createdUser.getId());
        assertThat(createdUser.getId()).isEqualTo(userFound.getId());
    }

    @Test
    void shouldThrow_getById() {
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnAllUsers_getAll() {
        UserDto userDto = new UserDto(null, "Пользователь", "owner@mail.ru");
        UserDto createdUser = userService.createUser(userDto);
        UserDto userDto1 = new UserDto(null, "Пользователь два", "mail@mail.ru");
        UserDto createdUser1 = userService.createUser(userDto1);
        List<UserDto> users = userService.getAll();
        assertThat(users).hasSize(2);
    }
}
