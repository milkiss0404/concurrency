package com.example.concurrency.item.Redis;

import com.example.concurrency.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLettuceService {

    private final static int TIME_OUT = 3000;
    private final RedisLettuceStructure redisLettuceStructure;
    private final ItemService itemService;

    public void buyItem(final Long key, final Long quantity) throws InterruptedException {
        // SpinLock 방식
        while (!redisLettuceStructure.generateLock(key, TIME_OUT)) {
            // SpinLock 으로 redis 부하를 줄여주기위한 sleep
            Thread.sleep(100);
        }

        // lock 획득 성공시 로직 수행
        try {
            itemService.buyItem(key, quantity);
        } finally {
            // lock 해제
            redisLettuceStructure.deleteLock(key);
        }
    }
}