package com.cts.corda.etf.flow.depository;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.corda.etf.contract.*;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference0Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KProperty;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import java.util.stream.Collectors;

import com.cts.corda.etf.util.*;
import net.corda.testing.*;


@StartableByRPC
@InitiatingFlow
@Slf4j
@CordaSerializable
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
        log.info("Called DepositoryExternalSellFlow for quantity " + quantity + " securityName " + securityName);
        PartyAndReference issuer = this.getOurIdentity().ref(OpaqueBytes.of((securityName + quantity).getBytes()));
        SecurityStock.State etfTradeState = new SecurityStock.State(issuer, getOurIdentity(), securityName, quantity.longValue());
        Party seller = getPartyWithName();
        log.info("Called DepositoryExternalSellFlow for seller " + seller);
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        SecuritySellState securitySellState = new SecuritySellState(quantity, securityName, Constants.SELL_STARTED, seller, getOurIdentity());

        final Command<SellContract.Commands.Create> txCommand = new Command<>(new SellContract.Commands.Create(),
                securitySellState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(securitySellState,
                SellContract.SELL_SECURITY_CONTRACT_ID), txCommand);

        log.info("etfTradeState -->> " + etfTradeState);
        txBuilder.verify(getServiceHub());
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        log.info("Inside EtfIssue flow finalize tx");
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        return notarisedTx;


    }

    private Party getPartyWithName() {
        return TestConstants.getDUMMY_BANK_A() ;
    }
}
