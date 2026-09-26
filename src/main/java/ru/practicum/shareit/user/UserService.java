package ru.practicum.shareit.user;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto userDto);

    UserDto updateUser(Long id, UserDto userDto);

    UserDto getById(Long id);

    List<UserDto> getAll();

    void delete(Long id);
}
