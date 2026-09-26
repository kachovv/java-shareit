package ru.practicum.shareit.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        validateUserDto(userDto);
        checkEmailExists(userDto.getEmail());
        User user = UserMapper.toUser(userDto);
        User saved = userRepository.save(user);
        log.info("Добавлен пользователь с id = {}", user.getId());
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = getUserOrThrow(id);
        if (userDto.getEmail() != null) {
            checkEmailForUpdate(userDto.getEmail(), id);
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        userRepository.save(user);
        log.info("Обновлен пользователь с id = {}", id);
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto getById(Long id) {
        User user = getUserOrThrow(id);
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        List<User> userList = userRepository.findAll();
        return userList.stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    public void delete(Long id) {
        getUserOrThrow(id);
        log.info("Удален пользователь с id = {}", id);
        userRepository.deleteById(id);
    }

    private void validateUserDto(UserDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Имя не может быть пустым");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (!dto.getEmail().contains("@")) {
            throw new ValidationException("Некорректный формат email");
        }
    }

    private void checkEmailExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " уже используется");
        }
    }

    private void checkEmailForUpdate(String email, Long currentUserId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ConflictException("Email " + email + " уже используется");
            }
        });
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }
}