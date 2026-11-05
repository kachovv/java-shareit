package ru.practicum.shareit.item.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Map;

@Service
public class ItemClient extends BaseClient {
    private static final String API_PREFIX = "/items";

    private ItemClient(RestTemplate rest) {
        super(rest);
    }

    public ResponseEntity<Object> create(Long userId, ItemDto itemDto, Long requestId) {
        String path = API_PREFIX;
        if (requestId != null) {
            path += "?requestId=" + requestId;
        }
        return post(path, userId, itemDto);
    }

    public ResponseEntity<Object> update(Long userId, Long itemId, ItemDto itemDto) {
        return patch(API_PREFIX + "/" + itemId, userId, itemDto);
    }

    public ResponseEntity<Object> getById(Long userId, Long itemId) {
        return get(API_PREFIX + "/" + itemId, userId);
    }

    public ResponseEntity<Object> getAllByOwner(Long userId) {
        return get(API_PREFIX, userId);
    }

    public ResponseEntity<Object> search(String text) {
        return get(API_PREFIX + "/search", null, Map.of("text", text));
    }

    public ResponseEntity<Object> addComment(Long userId, Long itemId, CommentDto commentDto) {
        return post(API_PREFIX + "/" + itemId + "/comment", userId, commentDto);
    }
}
