package com.cts.corda.etf.flow.sell;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.flow.buy.APBuyCompletionFlow;
import com.cts.corda.etf.flow.regulator.ReportToRegulatorFlow;
import com.cts.corda.etf.state.SecuritySellState;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

import java.util.List;

@InitiatingFlow
@InitiatedBy(APBuyCompletionFlow.class)
@Slf4j
public class ApSellSettleFlow extends FlowLogic<SignedTransaction> {

    private final FlowSession flowSession;

    public ApSellSettleFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        log.info("Inside ApSellSettleFlow called by " + flowSession.getCounterparty());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("Inside ApSellSettleFlow call " + flowSession.getCounterparty());
        List<StateAndRef<SecurityStock.State>> etfTradeStatesQueryResp = getServiceHub().getVaultService().queryBy(SecurityStock.State.class).getStates();
        StateAndRef<SecurityStock.State> stateAndRef = null;
        for (StateAndRef<SecurityStock.State> stateAndRef1 : etfTradeStatesQueryResp) {
            stateAndRef = stateAndRef1;
        }

        SignedTransaction fullySignedTx = subFlow(new MoveSecurityFlow(stateAndRef, flowSession.getCounterparty()));

        //UPDATE sell request as matched
        List<StateAndRef<SecuritySellState>> ref = getServiceHub().getVaultService().queryBy(SecuritySellState.class).getStates();
        SecuritySellState securitySellState = null;

        for (StateAndRef<SecuritySellState> stateref : ref) {
            securitySellState = stateref.getState().getData();
        }

        if (securitySellState != null) {
            //update sell state
            securitySellState.setBuyer(flowSession.getCounterparty());
            SignedTransaction fullySignedTx2 = subFlow(new UpdateSellRequestToMatch(securitySellState));
            //Report to regulator
            subFlow(new ReportToRegulatorFlow(fullySignedTx2));
        }

        return fullySignedTx;
    }


}

