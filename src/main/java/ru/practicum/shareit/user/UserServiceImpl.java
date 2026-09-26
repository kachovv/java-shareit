package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

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

    private void checkEmailExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email " + email + " уже используется");
        }
    }

    private void checkEmailForUpdate(String email, Long currentUserId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ValidationException("Email " + email + " уже используется");
            }
        });
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }
}