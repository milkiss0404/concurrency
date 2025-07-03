package com.example.concurrency.item;

import com.example.concurrency.item.Redis.RedisLettuceService;
import com.example.concurrency.item.Redis.RedissonService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemTest {
    private static final int TIME_OUT = 120;
    private static final Logger log = LoggerFactory.getLogger(ItemTest.class);
    @Autowired
    ItemService itemService;
    @Autowired
    ItemComponent itemComponent;

    @Autowired
    RedissonService redissonService;
    @Autowired
    RedisLettuceService redisLettuceService;
    @Autowired
    ItemRepository itemRepository;
    private ExecutorService executorService;


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 상품저장() {
        Item item = Item.builder()
                .name("청소기")
                .stock(1000)
                .build();
        itemRepository.save(item);
    }


    @Test
    void 동시에_123개의_상품구매() throws InterruptedException {

        final int threadCount = 123 ;  // 123개 구매 시도
        executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                itemService.buyItem(18L,1L);  // 트랜잭션 + 재시도 처리된 메서드
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);

        Item item = itemRepository.findById(18L)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        assertThat(item.getStock()).isEqualTo(0);
    }

    @Test
    void 동시에_1000개_상품구매_Whith_MySql_named_Lock() throws InterruptedException {
        final int threadCount = 1000 ;  // 1000개 구매 시도
        CountDownLatch latch = new CountDownLatch(threadCount);
        executorService = Executors.newFixedThreadPool(32);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    itemComponent.concurrencyWithNamedLock(27L,1L);
                }catch (Exception e){
                    log.error("에러",e);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);

        Item item = itemRepository.findById(27L)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        assertThat(item.getStock()).isEqualTo(0);
    }

    @Test
    void 동시에_1000개_상품구매_Whith_Redis_Lettuce() throws InterruptedException {
        final int threadCount = 1000 ;  // 1000개 구매 시도
        CountDownLatch latch = new CountDownLatch(threadCount);
        executorService = Executors.newFixedThreadPool(32);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                   redisLettuceService.buyItem(29L,1L);
                }catch (Exception e){
                    log.error("에러",e);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);

        Item item = itemRepository.findById(27L)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        assertThat(item.getStock()).isEqualTo(0);
    }
    @Test
    void 동시에_1000개_상품구매_Whith_Redison() throws InterruptedException {
        final int threadCount = 1000 ;  // 1000개 구매 시도
        CountDownLatch latch = new CountDownLatch(threadCount);
        executorService = Executors.newFixedThreadPool(32);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                  redissonService.buyItem(30L,1L);
                }catch (Exception e){
                    log.error("에러",e);
                }
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);

        Item item = itemRepository.findById(30L)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        assertThat(item.getStock()).isEqualTo(0);
    }
}