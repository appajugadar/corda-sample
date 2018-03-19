package com.cts.corda.etf.listener;

import com.cts.corda.etf.cordainteface.FlowInvokerService;
import com.cts.corda.etf.util.CashIssueOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jms.Session;
import java.util.concurrent.ExecutionException;

import static com.cts.corda.etf.util.Constants.ORDER_QUEUE;


@Component
@Slf4j
public class ExternalMessageReceiver {

    @Autowired
    FlowInvokerService flowInvokerService;

    @PostConstruct
    void init() {
        log.info("Initialised ExternalMessageReceiver");
    }

    @JmsListener(destination = ORDER_QUEUE)
    public void receiveMessage(@Payload CashIssueOrder cashIssueOrder,
                               @Headers MessageHeaders headers,
                               Message message, Session session) {
        log.info("Received external Message <" + cashIssueOrder + ">");
        try {
            flowInvokerService.selfIssueCash(cashIssueOrder.getAmount(), cashIssueOrder.getCurrency());
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error {}", e);
            e.printStackTrace();
        }
    }
}
