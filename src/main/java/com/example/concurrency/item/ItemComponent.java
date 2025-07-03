package com.example.concurrency.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ItemComponent {
    private final UserLevelLockWithJdbcTemplate userLevelLockWithJdbcTemplate;
    private final ItemService itemService;

    @Transactional
    public void concurrencyWithNamedLock(Long id, Long quantity) {
        String lockName = "item_lock_" + id;
        userLevelLockWithJdbcTemplate.executeWithLock(
                lockName,
                30,
                () -> {
                    itemService.buyItem(id, quantity);
                    return null;
                }
        );
    }
}