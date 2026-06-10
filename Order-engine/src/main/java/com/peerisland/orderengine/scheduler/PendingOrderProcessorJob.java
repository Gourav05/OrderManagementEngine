package com.peerisland.orderengine.scheduler;

import com.peerisland.orderengine.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderProcessorJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${order.processing.fixed-delay-ms}")
    public void processPendingOrders() {
        int processed = orderService.processPendingOrders();
        if (processed > 0) {
            log.info("Moved {} pending orders to PROCESSING", processed);
        }
    }
}
