package com.cts.corda.etf.cordainteface;

import com.cts.corda.etf.flow.depository.DepositoryExternalBuyFlow;
import com.cts.corda.etf.flow.depository.DepositoryExternalSellFlow;
import com.cts.corda.etf.util.SecurityOrder;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.AbstractCashFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class FlowInvokerService {

    @Autowired
    private NodeRPCConnection rpc;

    public String selfIssueCash(Integer amount, String currency) throws ExecutionException, InterruptedException {
        final List<Party> notaries = rpc.getProxy().notaryIdentities();

        FlowHandle<AbstractCashFlow.Result> flowHandle = rpc.getProxy().startFlowDynamic(net.corda.finance.flows.CashIssueFlow.class,
                new Amount<Currency>(amount * 100, Currency.getInstance(currency)),
                OpaqueBytes.of("40".getBytes()),
                notaries.get(0));

        AbstractCashFlow.Result result = flowHandle.getReturnValue().get();
        log.info("Received resp from flow " + result.getRecipient());
        return "SUCCESS";
    }

    public String initiateBuySellRequest(SecurityOrder securityOrder) throws ExecutionException, InterruptedException {
        String requesterPartyName = getPartyNameFromBic(securityOrder.getCounterPartyBic());
        log.info("requesterPartyName " + requesterPartyName);

        FlowProgressHandle<SignedTransaction> flowHandle = null;
        if (securityOrder.getBuySellIndicator().equals("BUY")) {
            flowHandle = rpc.getProxy().startTrackedFlowDynamic(DepositoryExternalBuyFlow.class, securityOrder.getQuantity(), securityOrder.getSecurityName(), requesterPartyName);
        } else {
            flowHandle = rpc.getProxy().startTrackedFlowDynamic(DepositoryExternalSellFlow.class, securityOrder.getQuantity(), securityOrder.getSecurityName(), requesterPartyName);
        }

        log.info("requesterPartyName " + requesterPartyName);
        flowHandle.getProgress().subscribe(evt -> log.info(">> %s\n", evt));
        final SignedTransaction result = flowHandle.getReturnValue().get();
        final String msg = String.format("Transaction id %s committed to ledger.\n", result.getId());
        return msg;
    }


    private Party getPartyWithName(CordaX500Name x500Name) {
        return rpc.getProxy().wellKnownPartyFromX500Name(x500Name);//;partiesFromName(name, false).toArray()[0];
    }

    String getPartyNameFromBic(String counterPartyBic) {
        Properties props = new Properties();
        try {
            try (InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("swiftbic-partyname.properties")) {
                props.load(resourceStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props.getProperty(counterPartyBic);
    }
}
