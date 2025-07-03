package com.example.concurrency.item;


import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final LockRepository lockRepository;
    private final DecreaseItemService decreaseItemService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void buyItem(Long id, Long quantity) {
                Item item = itemRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
            item.decreaseStock(quantity);
    }
    @Transactional
    public void decrease(Long id, Long quantity){
        try {
            //lock 획득
            lockRepository.getLock(id.toString());
            //재고 감소
            decreaseItemService.decreaseItem(id,quantity);
        }finally {
            //모든 로직이 종료되었을 때, lock 해제
            lockRepository.releaseLock(id.toString());
        }
    }
}