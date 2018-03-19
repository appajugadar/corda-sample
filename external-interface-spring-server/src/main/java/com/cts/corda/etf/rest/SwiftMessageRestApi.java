package com.cts.corda.etf.rest;

import com.cts.corda.etf.services.OrderSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
@RequestMapping("/api/swiftpost")
public class SwiftMessageRestApi {

    @Autowired
    OrderSender orderSender;

    @PostMapping(value = "/send")
    public String postCustomer(@RequestBody String swiftmessage) {
        log.info("swiftmessage " + swiftmessage);
        orderSender.send(swiftmessage);
        return "Post Successfully!";
    }
}
