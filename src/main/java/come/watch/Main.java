package come.watch;

import come.watch.common.RumDailyAggJob;
import come.watch.common.RumDailyTopNJob;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ApplicationRunner rumDailyAggOnceRunner(RumDailyAggJob rumDailyAggJob) {
        return args -> rumDailyAggJob.run();
    }

    @Bean
    public ApplicationRunner rumDailyTopNOnceRunner(RumDailyTopNJob rumDailyTopNJob) {
        return args -> rumDailyTopNJob.runDaily();
    }
}
