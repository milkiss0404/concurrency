package com.example.concurrency.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DecreaseItemService {
    private final ItemRepository itemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void decreaseItem(Long id, Long quantity){
        Item item = itemRepository.findById(id).orElseThrow();

        // 재고를 감소
        item.decreaseStock(quantity);

        // 갱신된 값을 저장
        itemRepository.saveAndFlush(item);
    }
}

