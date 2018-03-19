package com.cts.corda.etf.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecurityOrder {
    private String counterPartyBic;
    private String buySellIndicator;
    private String securityName;
    private Integer quantity;
}
