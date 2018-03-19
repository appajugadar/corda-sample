package com.cts.corda.etf.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CashIssueOrder implements Serializable {
    private Integer amount;
    private String currency;
}