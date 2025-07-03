# 동시성문제 해결하기 자바 , JPA ,Mysql Named Lock, Redis Lettuce, Redison

멀티 스레드를 사용하는 환경에서 각 스레드가 공유 자원에 동시에 접근하는 상황이라면 경쟁상태(Race condition)가 발생할 수 있습니다. 경쟁상태가 발생하게 되는 원인은 가시성(Visibility)과 원자성(Mutual Exclusion)을 보장하지 못했기 때문입니다. Java에서는 synchronized 키워드와 Atomic Type, Concurrent Collection 등을 통해 이와 같은 동시성 문제를 해결할 수 있습니다.

동시성 프로그래밍에서 발생할 수 있는 문제는?

동시성 프로그램에서는 CPU와 RAM의 중간에 위치하는 CPU Cache Memory와 병렬성이라는 특징때문에 가시성 문제, 원자성 문제가 발생할 수 있다.

가시성문제는

여러 개의 스레드가 사용됨에 따라, CPU Cache Memory와 RAM의 데이터가 서로 일치하지 않아 생기는 문제를 의미한다. 
가시성을 보장되어야 하는 변수에 volatile 키워드를 붙여줘서 RAM에서 바로 읽도록 해야 한다.
그러나 여러 스레드가 공유 자원에 쓰기 연산을 할 경우 가시성을 보장했다고 해서 동시성이 보장되지 않는다. 

즉 여러스레드가 같은 공유 변수에 접근할때, cpu캐시에 저장된 값과 실제 RAM에 저장된 값이 서로 다를떄

ex: 스레드 1이 CPU캐시에서 변수값을 수정,

값은 RAM에 바로반영이되지않는다 → 스레드2는 변경 이전의 값을 읽는다.

원자성문제 : 

공유되는 변수를 변경할때 기존의 값을 기반으로 새로운 값이 결정되는 과정에서 여러 스레드가 이를 동시에 수행할때 생기는 , 연산전체가 원자적으로 수행되지 않아 잘못된 결과가 발생하는 문제

여러 스레드가 공유 자원에 동시에 쓰기 연산을 할 경우 잘못된 결과를 반환하는 것을 의미한다. 따라서 synchronized, atomic 을 통해 원자성을 보장해야 한다.

자바에서 동시성을 해결하기위해서는 synchronized, volatile, Lock 인터페이스, Atomic 클래스등 이 있습니다 이중에 synchronized와 Lock 인터페이스로 동시성을 해결해보겠습니다.

# Thread Control

물론 Thread.sleep 으로 컨트롤할수있겠지만 비동기 작업, 병렬 처리, 쓰레드 관리에는 ExecutorService인터페이스가 유리합니다  

`ExecutorService` 인터페이스로 스레드를 컨트롤할수있습니다. 동시에 카운터가  1씩 100개의 스레드를 동시에 실행시켜 정확히 100 이오르는지 확인하겠습니다.

실패하는코드
```
    @Test
    void originalTest_Fail() throws Exception {
        int loop = 100;
        executorService = Executors.newFixedThreadPool(loop);
      
        for (int i = 0; i < loop; i++) {
            executorService.execute(() -> counter.increase());
        }
        executorService.shutdown();
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);
        Assertions.assertThat(counter.getCount()).isNotEqualTo(loop);
    }
```



성공 -synchronized 로 원자성을 보장받아 동시성을 해결할수있습니다.

```
@Test
    void synchronizedTest_Success() throws Exception {
        int loop = 100;
        executorService = Executors.newFixedThreadPool(loop);

        for (int i = 0; i < loop; i++) {
            executorService.execute(() -> synchronizedIncrease());
        }
        executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);

        Assertions.assertThat(counter.getCount()).isEqualTo(loop);
    }
```
ReentrantLock 설명

ReentrantLock은 재진입 가능한 락으로, 한 스레드가 이미 확보한 락을 다시 요청할 수 있게 해줍니다. 이는 데드락을 방지하고, 복잡한 동기화 상황에서 유용하게 사용됩니다.

왜냐하면 ReentrantLock을 사용하면 락을 획득하고 해제하는 과정을 개발자가 직접 제어할 수 있기 때문입니다. 이는 락의 범위와 시점을 더 세밀하게 관리할 수 있게 해줍니다.
![image](https://github.com/user-attachments/assets/3d0a435d-ca20-4eeb-a114-ef6c27fe4886)


synchronized 와 ReentrantLock의 차이점


synchronized
- synchronized 블럭으로 동기화를 하면 자동적으로 lock이 잠기고 풀립니다.(synchronized 블럭 내에서 예외가 발생해도 lock은 자동적으로 풀립니다.)
- 그러나 같은 메소드 내에서만 lock을 걸 수 있다는 제약이 존재합니다.
- 암묵적인 lock 방식
- WAITING 상태인 스레드는 interrupt가 불가능합니다.

ReentrantLock
- synchronized와 달리 수동으로 lock을 잠그고 해제해야 합니다.
- 명시적인 lock 방식
- 암묵적인 락만으로는 해결할 수 없는 복잡한 상황에서 사용할 수 있습니다.`
- lockInterruptably() 함수를 통해 WAITING 상태의 스레드를 interrupt할 수 있습니다.`

 
## 하지만 `synchronized` ,ReentrantLock은 자바 JVM 내의 **동기화 문제**만 해결할 수 있을 뿐, **DB의 트랜잭션 수준 동시성 문제**는 해결할 수 없습니다

결국 근본적인 해결책이 될 수 없었습니다

JPA락은 DB수준의 적용범위를 가지고, 트랜잭션간 데이터 정합성을 보장하고, 락의 범위가 메모리 내 가아닌, 실제 DB행이라 적절하다고 생각했습니다.





# JPA에서 동시성 해결하기

# **1. 낙관적 락(Optimistic Lock)**

대부분의 트랜잭션은 충돌이 발생하지 않는다고 낙관적으로 가정하는 방법이다.

낙관적 락은 비관적락과 다르게 데이터베이스가 제공하는 락이 아닌 **애플리케이션 레벨에서 락을 구현**하게 된다. JPA에서는 버전 관리 기능(`@Version`)을 통해 구현할 수 있다.

낙관적 락은 애플리케이션에서 충돌을 관리하기에 트랜잭션을 커밋하기 전까지는 충돌을 알 수 없다.

## **@Version**

JPA는 낙관적 락을 위해 `@Version` 어노테이션을 제공하고 있다. 해당 어노테이션이 붙은 필드를 포함하는 엔티티를 정의하면,
해당 엔티티 테이블을 읽는 각 트랜잭션은 업데이트를 수행하기 전에 버전의 속성을 확인하게 된다. 
만약 데이터를 읽고 업데이트를 하기 이전에 버전 값이 변경되어있다면 
`OptimisticLockException`을 발생 시키며 해당 업데이트를 취소하게 된다.
<p align="center">
![image (1)](https://github.com/user-attachments/assets/b671f6f7-5852-482a-ad97-fafdc5d79517)
</p>

**2. 비관적 락 @Lock(PESSIMISTIC_WRITE)**

- 비관적 잠금이란, 데이터를 읽은 후 변경할 가능성이 있는 경우, 데이터를 읽는 즉시 해당 데이터에 대한 잠금을 설정함으로써 다른 트랜잭션에서 해당 데이터를 변경하지 못하도록 하는 방법
- 해당 엔티티를 읽는 즉시 쓰기 잠금이 설정되어, 해당 엔티티를 읽은 트랜잭션이 완료될 때까지 다른 트랜잭션에서는 해당 엔티티를 읽거나 쓸 수 없게 됨

![image (2)](https://github.com/user-attachments/assets/c82c4b17-5c7a-443e-9aa6-be5ccdee81ce)
![image (3)](https://github.com/user-attachments/assets/c5b3cede-1f70-4405-ab8d-8fc63c0670ee)
![image (4)](https://github.com/user-attachments/assets/248197a2-f8db-4b3f-b45a-938d186073d1)


### 🔒 LockModeType 종류 정리

| LockModeType                | 설명 |
|----------------------------|------|
| `NONE`                     | ▪ 잠금을 사용하지 않음<br>▪ 기본값 |
| `OPTIMISTIC`               | ▪ 낙관적 잠금 사용<br>▪ 데이터 변경 시 다른 트랜잭션에 의한 변경 확인<br>▪ `@Version` 애노테이션을 사용하여 버전 컬럼 관리 |
| `OPTIMISTIC_FORCE_INCREMENT` | ▪ 낙관적 잠금 사용<br>▪ 잠금이 걸린 엔티티의 버전을 강제로 증가시킴<br>▪ 다른 트랜잭션에서 해당 엔티티를 읽을 때 충돌을 일으킴 |
| `PESSIMISTIC_READ`         | ▪ 비관적 잠금 사용<br>▪ 엔티티를 읽은 트랜잭션이 완료될 때까지 다른 트랜잭션에서 해당 엔티티 변경 방지<br>▪ 다른 트랜잭션에서는 해당 엔티티를 읽을 수 있음 |
| `PESSIMISTIC_WRITE`        | ▪ 비관적 잠금 사용<br>▪ 엔티티를 읽은 트랜잭션이 완료될 때까지 다른 트랜잭션에서 해당 엔티티를 읽거나 쓸 수 없게 함 |
| `PESSIMISTIC_FORCE_INCREMENT` | ▪ 비관적 잠금 사용<br>▪ 잠금이 걸린 엔티티의 버전을 강제로 증가시킴<br>▪ 다른 트랜잭션에서 해당 엔티티를 읽을 때 충돌을 일으킴 |

3. @Transactional isolation level을 조절해서 트랜잭션 접근 및 활동을 제한하는 방법
### 🧱 트랜잭션 격리 단계 (Isolation Level)

| 격리 단계 (Isolation Level) | 설명 |
|-----------------------------|------|
| `READ_UNCOMMITTED` | ▪ 가장 낮은 격리 수준<br>▪ 다른 트랜잭션에서 **아직 커밋되지 않은 데이터(Dirty Read)** 도 읽을 수 있음<br>▪ Dirty Read, Non-Repeatable Read, Phantom Read 모두 발생 가능<br>▪ 기본 격리 수준이며, 추가 정보 없이도 트랜잭션 처리 |
| `READ_COMMITTED` | ▪ 다른 트랜잭션에서 **커밋된 데이터만 읽음**<br>▪ Dirty Read 방지<br>▪ **Non-Repeatable Read, Phantom Read 발생 가능**<br>▪ `SELECT ... FOR UPDATE` 사용 시 읽은 데이터에 대한 잠금 가능 |
| `REPEATABLE_READ` | ▪ 같은 트랜잭션 내에서 여러 번 읽어도 항상 같은 결과 보장<br>▪ **Dirty Read, Non-Repeatable Read 방지**<br>▪ Phantom Read는 발생 가능<br>▪ `SELECT ... FOR UPDATE` 사용 가능<br>▪ 트랜잭션 시작 시점의 스냅샷(ReadView)을 사용하여 다른 트랜잭션의 변경 영향을 받지 않음 |
| `SERIALIZABLE` | ▪ 가장 높은 격리 수준<br>▪ 트랜잭션들을 **순차적으로 실행**하여 동시성 문제 완전 방지<br>▪ **모든 동시성 문제 방지** (Dirty, Non-Repeatable, Phantom 모두 방지)<br>▪ `SELECT ... FOR UPDATE` 사용 가능<br>▪ ReadView 사용<br>▪ 모든 쿼리에 SERIALIZABLE 옵션을 강제 적용 |


## mysql Named Lock

테이블이나 레코드, 데이터베이스 객체가 아닌 사용자가 지정한 문자열에 대해 락을 획득하고 반납하는 잠금으로, 한 세션이 Lock을 획득한다면, 다른 세션은 해당 세션이 Lock을 해제한 이후 획득할 수 있다. Lock에 이름을 지정하여 어플리케이션 단에서 제어가 가능하다.

Named Lock은 Redis를 사용하기 위한 인프라 구축, 유지보수 비용을 발생하지 않고, MySQL 을 사용해 분산 락을 구현할 수 있다. MySQL 에서는 getLock()을 통해 획득, releaseLock()으로 해지할 수 있다.

단점으로는 Lock이 자동으로 해제되지 않기 때문에, 별도의 명령어로 해제를 수행해주거나 선점시간이 끝나야 해제하는 등 락의 획득,반납에 대한 로직을 철저하게 구현해야한다.

또한, 일시적인 락의 정보가 DB에 저장되고, 락을 획득,반납하는 과정에서 DB에 불필요한 부하가 있을 수 있습니다.

**Lock 획득/해제는 다른 컴포넌트에서**하고,

**비즈니스 로직(재고 감소)은 내부 서비스에서 `@Transactional(propagation = REQUIRES_NEW)`로 실행**하는 패턴은 **동시성 제어와 트랜잭션 분리**를 동시에 해결할 수 있는 패턴입니다


![image (5)](https://github.com/user-attachments/assets/22ae1735-15b9-4b97-81a1-1df691b856de)
![image (6)](https://github.com/user-attachments/assets/a89d1b9d-8ae1-48f0-a399-f32a41bc732b)
![image (7)](https://github.com/user-attachments/assets/cef29163-7ec0-47ff-b2f2-c3d464104187)


**Redis를 활용한 분산 락(Lettuce)**

**Setnx** 명령어를 활용하여 분산락을 구현한다. Setnx는 Lock을 획득하려는 스레드가 Lock 획득 가능여부의 확인을 반복적으로 시도하는 **스핀 락(Spin Lock) 방식**이다.

### Lettuce

- setnx 명령어를 활용하여 분산락 구현 가능
    - `setnx` : `SET if Not eXist`의 줄임말로, 특정 key에 value 값이 존재하지 않을 경우에 값을 설정(set)하는 명령어
- spin lock 방식
    - lock을 획득하려는 스레드가 lock을 사용할 수 있는지 반복적으로 확인하면서 lock 획득을 시도하는 방식
    - retry 로직을 개발자가 작성해주어야 합니다.
- Spring data redis를 이용하면 lettuce가 기본이기 때문에 별도의 라이브러리를 사용하지 않아도 됩니다.
- **Lettuce를 활용해 재고감소 로직 작성하기**
    - MySQL Named Lock과 비슷함
    - 세션 관리에 신경을 쓰지 않아도 됨

![image (8)](https://github.com/user-attachments/assets/a0a2d2c9-da9e-4897-8d80-f5fd128c6c2c)
![image (9)](https://github.com/user-attachments/assets/196f9a8f-2b1a-466f-a1ff-111ef13638cf)




  ### 스핀 락 방식으로 인한 문제점

스핀 락 방식이기 때문에 지속적으로 락의 획득을 시도하면서 Redis에 많은 부하가 생긴다. 이를 위해, 일정 시간만큼 sleep 하면서 개선하지만 역시 Redis에 부하가 생긴다.

또한, 계속 락을 획득하기 위해 시도하는 중, 락을 가지고 있는 스레드가 비정상적으로 종료되면서 무한 대기상태로 빠질 수 있다. 그래서 락을 획득하는 최대 허용시간이나 최대 허용횟수를 지정해야 한다.

### Redisson

- pub-sub 기반으로 Lock 구현 제공
    - 채널 하나를 만들고 lock을 점유중인 스레드가 lock 획득하려고 대기하는 스레드에게 해제를 알려주면, 안내를 받은 스레드가 lock 획득을 시도하는 방식
- lock 획득 재시도를 기본으로 제공
- **Redisson를 활용해 재고감소 로직 작성하기**
    - Redisson 라이브러리 추가하기
    - Redisson은 lock 관련 class를 라이브러리에서 제공해주기 때문에 별도의 repository를 작성하지 않아도 됩니다.
 

![image (10)](https://github.com/user-attachments/assets/5cb6fa36-f9e4-49bd-ab7d-fced46393cf9)
![image (11)](https://github.com/user-attachments/assets/6fa323cf-d57c-4cbc-86a3-18c97009dea1)

- 재시도가 필요한 경우에는 redisson 활용
- 재시도가 필요하지 않은 경우에는 lettuce 활용
