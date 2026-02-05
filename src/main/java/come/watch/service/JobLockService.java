package come.watch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobLockService {

    private final JdbcTemplate jdbcTemplate;

    public boolean tryLock(String key) {
        Integer got = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 0)", Integer.class, key);
        return got != null && got == 1;
    }

    public void unlock(String key) {
        jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, key);
    }
}

