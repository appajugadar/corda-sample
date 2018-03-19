package com.cts.corda.etf.api;

import com.cts.corda.etf.contract.SecurityContract;
import com.cts.corda.etf.contract.SecurityStock;
import com.cts.corda.etf.flow.buy.APBuyFlow;
import com.cts.corda.etf.flow.commercialpaper.CommercialPaperIssueFlow;
import com.cts.corda.etf.flow.commercialpaper.CommercialPaperMoveFlow;
import com.cts.corda.etf.flow.issue.SecurityIssueFlow;
import com.cts.corda.etf.flow.security.FungibleSecurityIssueFlow;
import com.cts.corda.etf.flow.security.Security;
import com.cts.corda.etf.flow.security.SecurityTokenIssueFlow;
import com.cts.corda.etf.flow.sell.APSellFlow;
import com.cts.corda.etf.state.SecurityBuyState;
import com.cts.corda.etf.state.SecuritySellState;
import com.cts.corda.etf.util.RequestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.AbstractCashFlow;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("rest")
@Slf4j
public class RestApi {

    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;
    private final List<String> serviceNames = ImmutableList.of("Controller", "Network Map Service");

    public RestApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }


    @GET
    @Path("BuyRequests")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<SecurityBuyState>> getBuyRequests() {
        return rpcOps.vaultQuery(SecurityBuyState.class).getStates();
    }

    @GET
    @Path("SellRequests")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<SecuritySellState>> getSellRequests() {
        List<StateAndRef<SecuritySellState>> ref = rpcOps.vaultQuery(SecuritySellState.class).getStates();
        return ref;
    }

    @GET
    @Path("UnMatchedSellRequests")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SecuritySellState> getUnMatchedSellRequests() {
        List<StateAndRef<SecuritySellState>> ref = rpcOps.vaultQuery(SecuritySellState.class).getStates();
        return RequestHelper.getUnmatchedSecuritySellState(ref);
    }

    @GET
    @Path("UnMatchedBuyRequests")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SecurityBuyState> getUnMatchedBuyRequests() {
        List<StateAndRef<SecurityBuyState>> ref = rpcOps.vaultQuery(SecurityBuyState.class).getStates();
        return RequestHelper.getUnmatchedSecurityBuyState(ref);
    }

    @GET
    @Path("security-balance")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SecurityBalance> checkSecurityStockBalance() {
        List<StateAndRef<SecurityStock.State>> etfTradeStatesQueryResp = rpcOps.vaultQuery(SecurityStock.State.class).getStates();
        Map<String, SecurityBalance> securityBalanceMap = new HashMap<>();
        for (StateAndRef<SecurityStock.State> stateAndRef : etfTradeStatesQueryResp) {
            SecurityStock.State etfTradeState = stateAndRef.getState().getData();
            if (securityBalanceMap.containsKey(etfTradeState.getSecurityName())) {
                SecurityBalance balance = securityBalanceMap.get(etfTradeState.getSecurityName());
                balance.setQuantity(balance.getQuantity() + etfTradeState.getQuantity());
            } else {
                securityBalanceMap.put(etfTradeState.getSecurityName(), new SecurityBalance(etfTradeState.getSecurityName(), etfTradeState.getQuantity()));
            }
        }
        List<SecurityBalance> ls = new ArrayList<>();
        ls.addAll(securityBalanceMap.values());
        return ls;

    }

    @GET
    @Path("cash-balance")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CashBalance> cashBalances() {
        Map<Currency, Amount<Currency>> balanceMap = net.corda.finance.contracts.GetBalances.getCashBalances(rpcOps);
        List<CashBalance> balances = new ArrayList<>();
        for (Amount<Currency> cash : balanceMap.values()) {
            CashBalance cashBalance = new CashBalance(cash.getToken().getCurrencyCode(), cash.getQuantity());
            balances.add(cashBalance);
            log.info("cashBalance " + cashBalance);
        }
        return balances;
    }

    @GET
    @Path("buy-security")
    public Response buySecurity(@QueryParam("quantity") int quantity, @QueryParam("securityName") String securityName) {
        if (quantity <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'security Quantity' must be non-negative.\n").build();
        }

        Party depositoryParty = getPartyWithName(new CordaX500Name("DTCC", "London", "GB"));

        try {
            FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
                    .startTrackedFlowDynamic(APBuyFlow.class, quantity, securityName, depositoryParty);
            flowHandle.getProgress().subscribe(evt -> log.info(">> %s\n", evt));

            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", result.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            log.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @GET
    @Path("sell-security")
    public Response sellSecurity(@QueryParam("quantity") int quantity, @QueryParam("securityName") String securityName) {
        if (quantity <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'security Quantity' must be non-negative.\n").build();
        }

        Party depositoryParty = getPartyWithName(new CordaX500Name("DTCC", "London", "GB"));

        try {
            FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
                    .startTrackedFlowDynamic(APSellFlow.class, quantity, securityName, depositoryParty);
            flowHandle.getProgress().subscribe(evt -> log.info(">> %s\n", evt));

            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", result.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            log.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @GET
    @Path("self-issue-cash")
    public Response selfIssueCash(@QueryParam(value = "amount") int amount, @QueryParam(value = "currency") String currency) throws ExecutionException, InterruptedException {
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        log.info("Issue self cash : amount " + amount + "   currency " + currency);


        FlowHandle<AbstractCashFlow.Result> flowHandle = rpcOps.startFlowDynamic(net.corda.finance.flows.CashIssueFlow.class,
                new Amount<Currency>(amount * 100, Currency.getInstance(currency)),
                OpaqueBytes.of("40".getBytes()),
                notaries.get(0));


        AbstractCashFlow.Result result = flowHandle.getReturnValue().get();
        log.info("Received resp from flow " + result.getRecipient());
        return Response.status(CREATED).entity("SUCCESS").build();
    }

    @GET
    @Path("self-issue-security")
    public Response selfIssueEtfStock(@QueryParam(value = "quantity") int quantity, @QueryParam(value = "securityName") String securityName) throws ExecutionException, InterruptedException {
        log.info("Inputs for self issue security  " + securityName + "   quantity " + quantity);
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(SecurityIssueFlow.class, new Long(quantity), securityName);

        SignedTransaction result = flowHandle.getReturnValue().get();
        log.info("Received resp from flow " + result);

        return Response.status(CREATED).entity("SUCCESS").build();
    }


    private Party getPartyWithName(CordaX500Name x500Name) {
        return rpcOps.wellKnownPartyFromX500Name(x500Name);//;partiesFromName(name, false).toArray()[0];
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> whoami() {
        return ImmutableMap.of("me", myLegalName.getX500Principal().getName());
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }


    @GET
    @Path("self-issue-cp")
    public Response selfIssueCP(@QueryParam(value = "quantity") int quantity, @QueryParam(value = "currency") String currency) throws ExecutionException, InterruptedException {
        log.info("Inputs for self issue currency  " + currency + "   quantity " + quantity);
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }

        Amount<Currency> faceValue = new Amount<Currency>(new Long(quantity), Currency.getInstance(currency));
        FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(CommercialPaperIssueFlow.class, faceValue, Instant.now());

        SignedTransaction result = flowHandle.getReturnValue().get();
        log.info("Received resp from flow " + result);

        return Response.status(CREATED).entity("SUCCESS").build();
    }


    @GET
    @Path("move-cp")
    public Response moveCP(@QueryParam(value = "receiverPartyName") String receiverPartyName) throws ExecutionException, InterruptedException {
        Party receiverParty = getPartyWithName(new CordaX500Name(receiverPartyName, "London", "GB"));
        FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(CommercialPaperMoveFlow.class, receiverParty);
        SignedTransaction result = flowHandle.getReturnValue().get();
        log.info("Received resp from flow " + result);
        return Response.status(CREATED).entity("SUCCESS").build();
    }

    @GET
    @Path("self-issue-commodity")
    public Response selfIssueCommodity(@QueryParam(value = "quantity") Integer quantity, @QueryParam(value = "commodityCode") String commodityCode, @QueryParam(value = "commodityDisplayName") String commodityDisplayName) throws ExecutionException, InterruptedException {
        log.info("Received resp from flow quantity {}  commodityCode {}  commodityDisplayName {}" + quantity, commodityCode, commodityDisplayName);
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(FungibleSecurityIssueFlow.class,
                quantity.longValue(), commodityCode, commodityDisplayName);

        log.info("Received resp from flow " + flowHandle.getReturnValue());
        return Response.status(CREATED).entity("SUCCESS").build();
    }


    @GET
    @Path("self-issue-com")
    public Response selfIssueCom(@QueryParam(value = "quantity") Integer quantity, @QueryParam(value = "commodityCode") String commodityCode, @QueryParam(value = "commodityDisplayName") String commodityDisplayName) throws ExecutionException, InterruptedException {
        log.info("Received resp from flow quantity {}  commodityCode {}  commodityDisplayName {}" + quantity, commodityCode, commodityDisplayName);
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        //private val amount: Amount<Issued<Security>>,
        //                             private val issuerBankPartyRef: OpaqueBytes,
        //                             private val notary: Party


        Party ap1 = getPartyWithName(new CordaX500Name("AP1", "London", "GB"));
        PartyAndReference issuer = ap1.ref(OpaqueBytes.of((commodityCode + commodityDisplayName + quantity).getBytes()));
        Security commodity = new Security(commodityCode, commodityDisplayName, 0);
        Amount<Issued<Security>> commodityAmount = new Amount(quantity, new Issued<>(issuer, commodity));

        FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(SecurityTokenIssueFlow.class,
                commodityAmount,
                OpaqueBytes.of((commodityCode + commodityDisplayName + quantity).getBytes()),
                notaries.get(0));

        log.info("Received resp from flow " + flowHandle.getReturnValue());
        return Response.status(CREATED).entity("SUCCESS").build();
    }

    @GET
    @Path("commodity-balance")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SecurityContract.State> checkCommodityBalance() {
        List<StateAndRef<SecurityContract.State>> etfTradeStatesQueryResp = rpcOps.vaultQuery(SecurityContract.State.class).getStates();
        List<SecurityContract.State> securityBalanceMap = new ArrayList<>();
        for (StateAndRef<SecurityContract.State> stateAndRef : etfTradeStatesQueryResp) {
            securityBalanceMap.add(stateAndRef.getState().getData());
        }
        return securityBalanceMap;
    }

}