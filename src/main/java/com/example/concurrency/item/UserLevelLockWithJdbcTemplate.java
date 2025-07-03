package com.example.concurrency.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class UserLevelLockWithJdbcTemplate {

private static final String GET_LOCK = "SELECT GET_LOCK(:userLockName, :timeoutSeconds)";
private static final String RELEASE_LOCK = "SELECT RELEASE_LOCK(:userLockName)";
private static final String EXCEPTION_MESSAGE = "LOCK 을 수행하는 중에 오류가 발생하였습니다.";

private final NamedParameterJdbcTemplate jdbcTemplate;

public UserLevelLockWithJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}

public <T> T executeWithLock(String userLockName,
                             int timeoutSeconds,
                             Supplier<T> supplier) {

    try {
        getLock(userLockName, timeoutSeconds);
        return supplier.get();
    } finally {
        releaseLock(userLockName);
    }
}

private void getLock(String userLockName,
                     int timeoutSeconds) {

    Map<String, Object> params = new HashMap<>();
    params.put("userLockName", userLockName);
    params.put("timeoutSeconds", timeoutSeconds);

    log.info("GetLock!! userLockName ], timeoutSeconds ]", userLockName, timeoutSeconds);
    Integer result = jdbcTemplate.queryForObject(GET_LOCK, params, Integer.class);
    checkResult(result, userLockName, "GetLock");
}

private void releaseLock(String userLockName) {

    Map<String, Object> params = new HashMap<>();
    params.put("userLockName", userLockName);

    log.info("ReleaseLock!! userLockName ]", userLockName);

    Integer result = jdbcTemplate.queryForObject(RELEASE_LOCK, params, Integer.class);

    checkResult(result, userLockName, "ReleaseLock");
}

private void checkResult(Integer result,
                         String userLockName,
                         String type) {
    if (result == null) {
        log.error("USER LEVEL LOCK 쿼리 결과 값이 없습니다. type = {}, userLockName = {}", type, userLockName);
        throw new RuntimeException(EXCEPTION_MESSAGE);
    }
    if (result != 1) {
        log.error("USER LEVEL LOCK 쿼리 결과 값이 1이 아닙니다. type = {}, result = {}, userLockName = {}", type, result, userLockName);
        throw new RuntimeException(EXCEPTION_MESSAGE);
    }
}
}