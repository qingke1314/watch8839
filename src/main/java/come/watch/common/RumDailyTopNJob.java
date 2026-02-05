package come.watch.common;

import come.watch.service.JobLockService;
import come.watch.service.RumSampleService;
import come.watch.service.RumTopNService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RumDailyTopNJob {
    private final RumTopNService topnService;
    private final RumSampleService sampleService;
    private final JobLockService jobLockService;

    @Scheduled(cron = "0 20 0 * * ?")
    public void runDaily() {

        // TODO 开发先用，后续要去掉 minusDays(1)
        LocalDate day = LocalDate.now();
        // .minusDays(1);

        // 多实例建议加锁（见下方 JobLockService）
        if (!jobLockService.tryLock("rum_daily_rollup:" + day)) {
            log.warn("Skip daily rollup, lock not acquired. day={}", day);
            return;
        }

        try {
            log.info("Daily rollup start. day={}", day);

            topnService.rebuildTopn(day);
            sampleService.rebuildSamples(day);

            log.info("Daily rollup done. day={}", day);
        } catch (Exception e) {
            log.error("Daily rollup failed. day={}", day, e);
            throw e;
        } finally {
            jobLockService.unlock("rum_daily_rollup:" + day);
        }
    }

}
