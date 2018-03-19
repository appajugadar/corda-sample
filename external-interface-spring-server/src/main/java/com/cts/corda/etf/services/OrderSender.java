package com.cts.corda.etf.services;

import com.cts.corda.etf.util.CashIssueOrder;
import com.cts.corda.etf.util.SecurityOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import static com.cts.corda.etf.util.Constants.ORDER_QUEUE;
import static com.cts.corda.etf.util.Constants.SECURITY_ORDER_QUEUE;

@Service
@Slf4j
public class OrderSender {

    @Autowired
    private JmsTemplate jmsTemplate;

    public void send(CashIssueOrder myMessage) {
        log.info("sending with convertAndSend() to queue <" + myMessage + ">");
        jmsTemplate.convertAndSend(ORDER_QUEUE, myMessage);
    }

    public void send(String myMessage) {
        log.info("sending with convertAndSend() to queue <" + myMessage + ">");
        jmsTemplate.convertAndSend(SECURITY_ORDER_QUEUE, myMessage);
    }
}
