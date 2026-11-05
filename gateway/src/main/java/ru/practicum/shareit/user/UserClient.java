package ru.practicum.shareit.user;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;

@Service
public class UserClient extends BaseClient {
    private static final String API_PREFIX = "/users";

    public UserClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(UserDto userDto) {
        return post(API_PREFIX, userDto);
    }

    public ResponseEntity<Object> update(Long userId, UserDto userDto) {
        return patch(API_PREFIX + "/" + userId, userId, userDto);
    }

    public ResponseEntity<Object> getById(Long userId) {
        return get(API_PREFIX + "/" + userId);
    }

    public ResponseEntity<Object> getAll() {
        return get(API_PREFIX);
    }

    public ResponseEntity<Object> delete(Long userId) {
        return delete(API_PREFIX + "/" + userId);
    }
}