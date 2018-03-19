package com.cts.corda.etf.services;

import com.cts.corda.etf.util.SecurityOrder;
import com.prowidesoftware.swift.model.field.Field;
import com.prowidesoftware.swift.model.field.Field20;
import com.prowidesoftware.swift.model.field.Field32A;
import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

@Service
@Slf4j
public class SwiftMessageProcessor {


    public SecurityOrder parseSwiftMessageToSecurityOrder(String message) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(message.getBytes());
        MT103 mt = MT103.parse(inputStream);
        SecurityOrder securityOrder = new SecurityOrder();
        securityOrder.setCounterPartyBic(mt.getSender());
        log.info("Sender: " + mt.getSender() + "    Receiver: " + mt.getReceiver());
        Field20 ref = mt.getField20();

        log.info(Field.getLabel(ref.getName(), mt.getMessageType(), null) + ": " + ref.getComponent(Field20.REFERENCE));
        Field32A f = mt.getField32A();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        log.info("Value Date: " + sdf.format(f.getDateAsCalendar().getTime()));
        securityOrder.setQuantity(Integer.parseInt(f.getAmount().replaceAll(",", "")));

        log.info("getField71A().getValue(): " + mt.getField71A().getValue());
        String securityName = mt.getField23B().getValue();
        log.info("securityName  " + securityName);
        securityOrder.setSecurityName(securityName);
        if(mt.getField71A().getValue().equals("BUY")){
            securityOrder.setBuySellIndicator("BUY");
        }else{
            securityOrder.setBuySellIndicator("SELL");
        }

        return securityOrder;
    }


}
