package com.cts.corda.etf.rest;

import com.cts.corda.etf.cordainteface.NodeRPCConnection;
import com.cts.corda.etf.services.OrderSender;
import com.cts.corda.etf.util.CashIssueOrder;
import com.cts.corda.etf.util.Constants;
import kotlin.collections.CollectionsKt;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@RestController
@RequestMapping("/transaction")
@Slf4j
public class ExternalRestApi {

    private final CordaX500Name myName;
    private final NodeRPCConnection rpc;
    private final String controllerName;

    @Autowired
    private OrderSender orderSender;

    public ExternalRestApi(@NotNull NodeRPCConnection rpc, @Value("${config.controller.name}") @NotNull String controllerName) {
        super();
        this.rpc = rpc;
        this.controllerName = controllerName;
        this.myName = ((Party) CollectionsKt.first(this.rpc.getProxy().nodeInfo().getLegalIdentities())).getName();
    }

    @GetMapping(produces = {"text/plain"}, value = {"/myname"})
    public final String myName() {
        return this.myName.toString();
    }

    @GetMapping(produces = {"application/json"}, value = {"/getcash"})
    public final Map<Currency, Amount<Currency>> getCashBalance() {
        Map<Currency, Amount<Currency>> balanceMap = net.corda.finance.contracts.GetBalances.getCashBalances(rpc.getProxy());
        return balanceMap;
    }

    @RequestMapping("/sendCashIssueOrder")
    public void sendCashIssueOrder(@RequestParam(value = "amount") Integer amount,
                                   @RequestParam(value = "currency") String currency) {
        orderSender.send(new CashIssueOrder(amount, currency));
    }


    @RequestMapping("/sendSecurityOrder")
    public void sendSecurityOrder(@RequestParam(value = "message") String message) {
        orderSender.send(message);
    }


    @GetMapping(produces = {"text/plain"}, value = {"/messages"})
    public final String getCordaNetworkMessages() throws JMSException {
        log.info("getCordaNetworkMessages");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost:61616?broker.persistent=true,useShutdownHook=false");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        ActiveMQQueue destination = new ActiveMQQueue(Constants.ORDER_QUEUE);
        QueueBrowser browser = session.createBrowser((Queue) destination);
        Enumeration enumeration = browser.getEnumeration();
        log.info("enumeration.hasMoreElements() --> "+enumeration.hasMoreElements());
        List<String> messages = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            TextMessage msg = (TextMessage) enumeration.nextElement();
            messages.add(msg.getText());
        }

        browser.close();
        if (messages.size() > 0)
            return messages.get(0);
        else
            return "NoMessage";
    }
}
