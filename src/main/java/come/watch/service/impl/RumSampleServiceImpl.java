package come.watch.service.impl;

import come.watch.service.RumSampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RumSampleServiceImpl implements RumSampleService {
    @Override
    public void rebuildSamples(LocalDate day) {
        log.info("Rebuild samples. day={}", day);
    }
}
