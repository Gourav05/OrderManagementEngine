package com.peerisland.orderengine.scheduler;

import com.peerisland.orderengine.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderProcessorJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${order.processing.fixed-delay-ms}")
    public void processPendingOrders() {
        List<String> processed = orderService.processPendingOrderIds();
        if (!processed.isEmpty()) {
            log.info("Moved {} pending orders to PROCESSING: {}", processed.size(), processed);
        }
    }
}
