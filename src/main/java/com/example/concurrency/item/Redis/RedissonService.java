package com.example.concurrency.item.Redis;

import com.example.concurrency.item.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Slf4j
public class RedissonService {

    private final RedissonClient redissonClient;
    private final ItemService itemService;

    public void buyItem(final Long key, final Long quantity) {
        
        // key 로 Lock 객체를 가져온다
        RLock lock = redissonClient.getLock(key.toString());

        try {
            // 획득 대기 타임아웃, 락 만료 시간
            boolean available = lock.tryLock(10, 2, TimeUnit.SECONDS);

            if (!available) {
                log.info("lock 획득 실패");
                return;
            }

            itemService.buyItem(key, quantity);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}