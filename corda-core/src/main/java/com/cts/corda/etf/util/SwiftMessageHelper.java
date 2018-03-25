package com.cts.corda.etf.util;

import com.prowidesoftware.swift.model.SwiftTagListBlock;
import com.prowidesoftware.swift.model.field.Field13A;
import com.prowidesoftware.swift.model.field.Field20C;
import com.prowidesoftware.swift.model.field.Field23G;
import com.prowidesoftware.swift.model.field.Field95P;
import com.prowidesoftware.swift.model.mt.mt5xx.MT517;

public class SwiftMessageHelper {

public static String createSwiftMessage(String otherPartyBic, String transactionId){

    MT517 mt = new MT517().append(MT517.SequenceA.newInstance(new SwiftTagListBlock()
            .append(Field20C.tag(":SEME//"+transactionId))
            .append(Field23G.tag("NEWM/CODU"))
            .append(Field95P.tag(":AFFM//MGTCDE55"))
            .append(MT517.SequenceA1.newInstance(
                    Field13A.tag("LINK//515"),
                    Field20C.tag(":RELA//FRTJ12CONF0002")))
            .append(MT517.SequenceA1.newInstance(
                    Field13A.tag("LINK//512"),
                    Field20C.tag(":RELA//FRTJ12CONF0003")))
    ));

    mt.setSender("FOOSEDR0AXXX");
    mt.setReceiver(otherPartyBic);
    return mt.message();
}
}
