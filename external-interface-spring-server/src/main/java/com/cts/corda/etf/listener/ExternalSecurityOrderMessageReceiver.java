package com.cts.corda.etf.listener;

import com.cts.corda.etf.cordainteface.FlowInvokerService;
import com.cts.corda.etf.services.SwiftMessageProcessor;
import com.cts.corda.etf.util.CashIssueOrder;
import com.cts.corda.etf.util.SecurityOrder;
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
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.cts.corda.etf.util.Constants.ORDER_QUEUE;
import static com.cts.corda.etf.util.Constants.SECURITY_ORDER_QUEUE;


@Component
@Slf4j
public class ExternalSecurityOrderMessageReceiver {

    @Autowired
    FlowInvokerService flowInvokerService;

    @Autowired
    SwiftMessageProcessor swiftMessageProcessor;

    @PostConstruct
    void init() {
        log.info("Initialised ExternalSecurityOrderMessageReceiver");
    }

    @JmsListener(destination = SECURITY_ORDER_QUEUE)
    public void receiveMessage(@Payload String messageText,
                               @Headers MessageHeaders headers,
                               Message message, Session session) {
        log.info("Received external Message <" + messageText + ">");
        try {
            SecurityOrder securityOrder = swiftMessageProcessor.parseSwiftMessageToSecurityOrder(messageText);
            flowInvokerService.initiateBuySellRequest(securityOrder);
        } catch (ExecutionException | InterruptedException | IOException e) {
            log.error("Error {}", e);
            e.printStackTrace();
        }
    }



}
