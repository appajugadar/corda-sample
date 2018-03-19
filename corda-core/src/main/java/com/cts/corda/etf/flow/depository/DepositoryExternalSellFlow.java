package com.cts.corda.etf.flow.depository;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.BuyContract;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.state.SecurityBuyState;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;

import java.util.stream.Collectors;

import static com.cts.corda.etf.contract.SellContract.SELL_SECURITY_CONTRACT_ID;
import static com.cts.corda.etf.util.Constants.SELL_STARTED;


@StartableByRPC
@InitiatingFlow
@Slf4j
public class DepositoryExternalSellFlow extends FlowLogic<SignedTransaction> {

    private Integer quantity;
    private String securityName;
    private String partyName;

    public DepositoryExternalSellFlow(Integer quantity, String securityName, String partyName) {
        super();
        this.quantity = quantity;
        this.securityName = securityName;
        this.partyName = partyName;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        log.info("Called DepositoryExternalBuyFlow for quantity " + quantity + " securityName " + securityName);
        PartyAndReference issuer = this.getOurIdentity().ref(OpaqueBytes.of((securityName + quantity).getBytes()));
        SecurityStock.State etfTradeState = new SecurityStock.State(issuer, getOurIdentity(), securityName, quantity.longValue());
        Party buyer = getPartyWithName(partyName);
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        SecurityBuyState securityBuyState = new SecurityBuyState(quantity, securityName, SELL_STARTED, buyer, getOurIdentity());

        final Command<BuyContract.Commands.Create> txCommand = new Command<>(new BuyContract.Commands.Create(),
                securityBuyState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securityBuyState,
                SELL_SECURITY_CONTRACT_ID), txCommand);

        log.info("etfTradeState -->> " + etfTradeState);
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        log.info("Inside EtfIssue flow finalize tx");
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        return notarisedTx;


    }

    private Party getPartyWithName(String partyName) {
        return getServiceHub().getIdentityService().wellKnownPartyFromX500Name(new CordaX500Name(partyName, "London", "GB"));
    }
}
