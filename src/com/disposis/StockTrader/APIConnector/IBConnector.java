package com.disposis.StockTrader.APIConnector;

import com.disposis.StockTrader.DataRetrievers.MessageQueues;
import com.disposis.StockTrader.Instruments.Contractable;
import com.disposis.StockTrader.Instruments.Stock;
import com.disposis.util.Logger;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.SoftDollarTier;
import com.ib.client.TickType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class IBConnector implements EWrapper {
	
	protected final static int MARKET_DATA_TYPE_DELAYED = 3;
	
	public final static int TICK_FIELD_DELAYED_LAST = 68;
	public final static int TICK_FIELD_DELAYED_CLOSE = 75;
	
	private static IBConnector instance;
	
	protected Map<Integer, MessageQueues> orderMessageQueues = new HashMap<>();
	protected Map<Integer, MessageQueues> messageQueues = new HashMap<>();
	
	private final EReaderSignal readerSignal;
	private final EClientSocket clientSocket;
	
	protected Logger logger;
	
	protected AtomicInteger currentOrderId = new AtomicInteger(-1);
	
	private IBConnector() {
		readerSignal = new EJavaSignal();
		clientSocket = new EClientSocket(this, readerSignal);
		
		this.logger = Logger.getInstance();
	}
	
	public void connect()
	{
		clientSocket.eConnect("127.0.0.1", 7497, 0);
		
		final EReader reader = new EReader(clientSocket, readerSignal);     
        reader.start();        
        
        new Thread() {
			@Override
        	public void run() {
        		while (clientSocket.isConnected()) {
        			readerSignal.waitForSignal();
    				try {
    					reader.processMsgs();
    				} catch (Exception e) {
    					System.out.println("Exception: "+e.getMessage() + "Exception type: " + e.getClass() + " Stack trace: " );
    					e.printStackTrace();
    				}
        		}
        	}
        }.start();
	}
	
	public void init()
	{	
		clientSocket.reqMarketDataType( IBConnector.MARKET_DATA_TYPE_DELAYED );
	}
	
	public void disconnect()
	{
		clientSocket.eDisconnect();
	}
	
	//! [socket_init]
	public EClientSocket getClient() {
		return clientSocket;
	}
	
	public EReaderSignal getSignal() {
		return readerSignal;
	}
	
	public int getCurrentOrderId() {
		return currentOrderId.get();
	}
	
	public int getAndIncrementCurrentOrderId() {
		return currentOrderId.getAndIncrement();
	}
	
	public void setMessageQueue( int tickerId, MessageQueues messageQueues )
	{
		if ( this.messageQueues.containsKey(tickerId) && this.messageQueues.get(tickerId) != messageQueues )
		{
			throw new IllegalArgumentException("IBConnector::getContractDetailsData() - Duplicate message queue for ticker id: " + tickerId );
		}
		else if ( !this.messageQueues.containsKey(tickerId) )
		{
			this.messageQueues.put(tickerId, messageQueues);
		}
	}
	
	public void removeMessageQueue( int tickerId )
	{
		messageQueues.remove(tickerId);
	}
	
	public void getContractData( int tickerId, Contractable contractor, MessageQueues messageQueues )
	{
		logger.trace("IBConnect::getContractData() - Entering getContractData().");
		
		if ( this.messageQueues.containsKey(tickerId) && this.messageQueues.get(tickerId) != messageQueues )
		{
			throw new IllegalArgumentException("IBConnector::getContractData() - Duplicate message queue for ticker id: " + tickerId );
		}
		else if ( !this.messageQueues.containsKey(tickerId) )
		{
			this.messageQueues.put(tickerId, messageQueues);
		}
		
		clientSocket.reqMktData(tickerId, contractor.getContract(), "", false, null);
	}
	
	public void cancelContractDataRequest( int tickerId ) 
	{
		clientSocket.cancelMktData(tickerId);
	}
	
	public void getContractDetailsData( int tickerId, MessageQueues messageQueues, Contractable contractor  )
	{	
		getContractDetailsData(tickerId, messageQueues, contractor.getContract());
	}
	
	public void getContractDetailsData( int tickerId, MessageQueues messageQueues, Contract contract )
	{
		if ( this.messageQueues.containsKey(tickerId) && this.messageQueues.get(tickerId) != messageQueues )
		{
			throw new IllegalArgumentException("IBConnector::getContractDetailsData() - Duplicate message queue for ticker id: " + tickerId );
		}
		else if ( !this.messageQueues.containsKey(tickerId) )
		{
			this.messageQueues.put(tickerId, messageQueues);
		}
		
		clientSocket.reqContractDetails(tickerId, contract);
	}
	
	public void getAvailableOptionsData( int tickerId, Stock stock, String exchange, MessageQueues messageQueues )
	{
		this.messageQueues.put(tickerId, messageQueues);
		
		clientSocket.reqSecDefOptParams(tickerId, stock.getSymbol(), exchange, stock.getSecurityType().getApiString(), stock.getContractId() );
	}
	
	public void placeOrder( int orderId, Contract contract, Order order, MessageQueues messageQueues )
	{
		this.orderMessageQueues.put(orderId, messageQueues);
		
		clientSocket.placeOrder(orderId, contract, order);
	}
	
	public void getExecutions( int tickerId, MessageQueues messageQueues  )
	{	
		getExecutions( tickerId, null, messageQueues );
	}
	
	public void getExecutions( int tickerId, ExecutionFilter filter, MessageQueues messageQueues  )
	{	
		if ( this.messageQueues.containsKey(tickerId) && this.messageQueues.get(tickerId) != messageQueues )
		{
			throw new IllegalArgumentException("IBConnector::getContractDetailsData() - Duplicate message queue for ticker id: " + tickerId );
		}
		else if ( !this.messageQueues.containsKey(tickerId) )
		{
			this.messageQueues.put(tickerId, messageQueues);
		}
		
		clientSocket.reqExecutions(tickerId, filter );
	}
	
	 //! [tickprice]
	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) 
	{
		logger.debug("IBConnector::tickPrice() - Tick Price. Ticker Id:"+tickerId+", Field: "+field+", Price: "+price+", CanAutoExecute: "+canAutoExecute);
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::tickPrice() - No message queue for ticker id:" + tickerId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new TickDataMessage(tickerId, field, price, canAutoExecute == 1) );
				messageQueues.notifyAll();
			}
			
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::contractDetails - InterruptedException caught in contractDetails. Message: " + e.getMessage() );
		}
	}
	//! [tickprice]
	
	//! [ticksize]
	@Override
	public void tickSize(int tickerId, int field, int size) {
//		System.out.println("Tick Size. Ticker Id:" + tickerId + ", Field: " + field + ", Size: " + size);
	}
	//! [ticksize]
	
	//! [tickoptioncomputation]
	@Override
	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta,
			double undPrice) {
		System.out.println("TickOptionComputation. TickerId: "+tickerId+", field: "+field+", ImpliedVolatility: "+impliedVol+", Delta: "+delta
                +", OptionPrice: "+optPrice+", pvDividend: "+pvDividend+", Gamma: "+gamma+", Vega: "+vega+", Theta: "+theta+", UnderlyingPrice: "+undPrice);
	}
	//! [tickoptioncomputation]
	
	//! [tickgeneric]
	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		System.out.println("Tick Generic. Ticker Id:" + tickerId + ", Field: " + TickType.getField(tickType) + ", Value: " + value);
	}
	//! [tickgeneric]
	
	//! [tickstring]
	@Override
	public void tickString(int tickerId, int tickType, String value) {
		System.out.println("Tick string. Ticker Id:" + tickerId + ", Type: " + tickType + ", Value: " + value);
	}
	//! [tickstring]
	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureLastTradeDate, double dividendImpact,
			double dividendsToLastTradeDate) {
		System.out.println("TickEFP. "+tickerId+", Type: "+tickType+", BasisPoints: "+basisPoints+", FormattedBasisPoints: "+
			formattedBasisPoints+", ImpliedFuture: "+impliedFuture+", HoldDays: "+holdDays+", FutureLastTradeDate: "+futureLastTradeDate+
			", DividendImpact: "+dividendImpact+", DividendsToLastTradeDate: "+dividendsToLastTradeDate);
	}
	//! [orderstatus]
	@Override
	public void orderStatus(int orderId, String status, double filled,
			double remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld) {
		logger.debug("OrderStatus. Id: "+orderId+", Status: "+status+", Filled"+filled+", Remaining: "+remaining
                +", AvgFillPrice: "+avgFillPrice+", PermId: "+permId+", ParentId: "+parentId+", LastFillPrice: "+lastFillPrice+
                ", ClientId: "+clientId+", WhyHeld: "+whyHeld);
		
		MessageQueues messageQueues = this.orderMessageQueues.get(orderId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::openOrder() - No message queue for order id:" + orderId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new OrderStatusMessage(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld ) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::openOrder - InterruptedException caught in openOrder. Message: " + e.getMessage() );
		}
	}
	//! [orderstatus]
	
	//! [openorder]
	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		logger.debug("OpenOrder. ID: "+orderId+", "+contract.symbol()+", "+contract.secType()+" @ "+contract.exchange()+": "+
			order.action()+", "+order.orderType()+" "+order.totalQuantity()+", "+orderState.status());
		
		MessageQueues messageQueues = this.orderMessageQueues.get(orderId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::openOrder() - No message queue for order id:" + orderId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new OpenOrderMessage(orderId, contract, order, orderState ) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::openOrder - InterruptedException caught in openOrder. Message: " + e.getMessage() );
		}
	}
	//! [openorder]
	
	//! [openorderend]
	@Override
	public void openOrderEnd() {
		System.out.println("OpenOrderEnd");
	}
	//! [openorderend]
	
	//! [updateaccountvalue]
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
		System.out.println("UpdateAccountValue. Key: " + key + ", Value: " + value + ", Currency: " + currency + ", AccountName: " + accountName);
	}
	//! [updateaccountvalue]
	
	//! [updateportfolio]
	@Override
	public void updatePortfolio(Contract contract, double position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		System.out.println("UpdatePortfolio. "+contract.symbol()+", "+contract.secType()+" @ "+contract.exchange()
                +": Position: "+position+", MarketPrice: "+marketPrice+", MarketValue: "+marketValue+", AverageCost: "+averageCost
                +", UnrealisedPNL: "+unrealizedPNL+", RealisedPNL: "+realizedPNL+", AccountName: "+accountName);
	}
	//! [updateportfolio]
	
	//! [updateaccounttime]
	@Override
	public void updateAccountTime(String timeStamp) {
		System.out.println("UpdateAccountTime. Time: " + timeStamp+"\n");
	}
	//! [updateaccounttime]
	
	//! [accountdownloadend]
	@Override
	public void accountDownloadEnd(String accountName) {
		System.out.println("Account download finished: "+accountName+"\n");
	}
	//! [accountdownloadend]
	
	//! [nextvalidid]
	@Override
	public void nextValidId(int orderId) {
		logger.trace("IBConnector::nextValidId() - Next Valid Id: ["+orderId+"]");
		
		currentOrderId.set(orderId);
	}
	//! [nextvalidid]
	
	//! [contractdetails]
	@Override
	public void contractDetails(int tickerId, ContractDetails contractDetails) {
		logger.debug("ContractDetails. ReqId: ["+tickerId+"] - ["+contractDetails.contract().symbol()+"], ["+contractDetails.contract().secType()+"], ConId: ["+contractDetails.contract().conid()+"] @ ["+contractDetails.contract().exchange()+"]");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::contractDetails() - No message queue for ticker id:" + tickerId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new ContractDetailsMessage(tickerId, contractDetails) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::contractDetails - InterruptedException caught in contractDetails. Message: " + e.getMessage() );
		}
	}
	//! [contractdetails]
	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		System.out.println("bondContractDetails");
	}
	//! [contractdetailsend]
	@Override
	public void contractDetailsEnd(int tickerId) {
		logger.debug("ContractDetailsEnd. "+tickerId+"\n");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::contractDetailsEnd() - No message queue for ticker id:" + tickerId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new StreamEndMessage(tickerId) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::contractDetailsEnd - InterruptedException caught in contractDetails. Message: " + e.getMessage() );
		}
	}
	//! [contractdetailsend]
	
	//! [execdetails]
	@Override
	public void execDetails(int tickerId, Contract contract, Execution execution) {
		logger.debug("ExecDetails. "+tickerId+" - ["+contract.symbol()+"], ["+contract.secType()+"], ["+contract.currency()+"], ["+execution.execId()+"], ["+execution.orderId()+"], ["+execution.shares()+"]");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.debug("IBConnector::execDetails() - No message queue for ticker id:" + tickerId );
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new ExecutionDetailsMessage(execution.orderId(), contract, execution) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::contractDetailsEnd - InterruptedException caught in contractDetails. Message: " + e.getMessage() );
		}
	}
	//! [execdetails]
	
	//! [execdetailsend]
	@Override
	public void execDetailsEnd(int reqId) {
		System.out.println("ExecDetailsEnd. "+reqId+"\n");
	}
	//! [execdetailsend]
	
	//! [updatemktdepth]
	@Override
	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) {
		System.out.println("UpdateMarketDepth. "+tickerId+" - Position: "+position+", Operation: "+operation+", Side: "+side+", Price: "+price+", Size: "+size+"");
	}
	//! [updatemktdepth]
	@Override
	public void updateMktDepthL2(int tickerId, int position,
			String marketMaker, int operation, int side, double price, int size) {
		System.out.println("updateMktDepthL2");
	}
	//! [updatenewsbulletin]
	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) {
		System.out.println("News Bulletins. "+msgId+" - Type: "+msgType+", Message: "+message+", Exchange of Origin: "+origExchange+"\n");
	}
	//! [updatenewsbulletin]
	
	//! [managedaccounts]
	@Override
	public void managedAccounts(String accountsList) {
		logger.message("Account list: " +accountsList);
	}
	//! [managedaccounts]

	//! [receivefa]
	@Override
	public void receiveFA(int faDataType, String xml) {
		System.out.println("Receing FA: "+faDataType+" - "+xml);
	}
	//! [receivefa]
	
	//! [historicaldata]
	@Override
	public void historicalData(int reqId, String date, double open,
			double high, double low, double close, int volume, int count,
			double WAP, boolean hasGaps) {
		System.out.println("HistoricalData. "+reqId+" - Date: "+date+", Open: "+open+", High: "+high+", Low: "+low+", Close: "+close+", Volume: "+volume+", Count: "+count+", WAP: "+WAP+", HasGaps: "+hasGaps);
	}
	//! [historicaldata]
	
	//! [scannerparameters]
	@Override
	public void scannerParameters(String xml) {
		System.out.println("ScannerParameters. "+xml+"\n");
	}
	//! [scannerparameters]
	
	//! [scannerdata]
	@Override
	public void scannerData(int reqId, int rank,
			ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {
		System.out.println("ScannerData. "+reqId+" - Rank: "+rank+", Symbol: "+contractDetails.contract().symbol()+", SecType: "+contractDetails.contract().secType()+", Currency: "+contractDetails.contract().currency()
                +", Distance: "+distance+", Benchmark: "+benchmark+", Projection: "+projection+", Legs String: "+legsStr);
	}
	//! [scannerdata]
	
	//! [scannerdataend]
	@Override
	public void scannerDataEnd(int reqId) {
		System.out.println("ScannerDataEnd. "+reqId);
	}
	//! [scannerdataend]
	
	//! [realtimebar]
	@Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		System.out.println("RealTimeBars. " + reqId + " - Time: " + time + ", Open: " + open + ", High: " + high + ", Low: " + low + ", Close: " + close + ", Volume: " + volume + ", Count: " + count + ", WAP: " + wap);
	}
	//! [realtimebar]
	@Override
	public void currentTime(long time) {
		System.out.println("currentTime");
	}
	//! [fundamentaldata]
	@Override
	public void fundamentalData(int reqId, String data) {
		System.out.println("FundamentalData. ReqId: ["+reqId+"] - Data: ["+data+"]");
	}
	//! [fundamentaldata]
	@Override
	public void deltaNeutralValidation(int reqId, DeltaNeutralContract underComp) {
		System.out.println("deltaNeutralValidation");
	}
	//! [ticksnapshotend]
	@Override
	public void tickSnapshotEnd(int reqId) {
		System.out.println("TickSnapshotEnd: "+reqId);
	}
	//! [ticksnapshotend]
	
	//! [marketdatatype]
	@Override
	public void marketDataType(int reqId, int marketDataType) {
//		logger.message("MarketDataType. ["+reqId+"], Type: ["+marketDataType+"]\n");
	}
	//! [marketdatatype]
	
	//! [commissionreport]
	@Override
	public void commissionReport(CommissionReport commissionReport) {
		System.out.println("CommissionReport. ["+commissionReport.m_execId+"] - ["+commissionReport.m_commission+"] ["+commissionReport.m_currency+"] RPNL ["+commissionReport.m_realizedPNL+"]");
	}
	//! [commissionreport]
	
	//! [position]
	@Override
	public void position(String account, Contract contract, double pos,
			double avgCost) {
		System.out.println("Position. "+account+" - Symbol: "+contract.symbol()+", SecType: "+contract.secType()+", Currency: "+contract.currency()+", Position: "+pos+", Avg cost: "+avgCost);
	}
	//! [position]
	
	//! [positionend]
	@Override
	public void positionEnd() {
		System.out.println("PositionEnd \n");
	}
	//! [positionend]
	
	//! [accountsummary]
	@Override
	public void accountSummary(int reqId, String account, String tag,
			String value, String currency) {
		System.out.println("Acct Summary. ReqId: " + reqId + ", Acct: " + account + ", Tag: " + tag + ", Value: " + value + ", Currency: " + currency);
	}
	//! [accountsummary]
	
	//! [accountsummaryend]
	@Override
	public void accountSummaryEnd(int reqId) {
		System.out.println("AccountSummaryEnd. Req Id: "+reqId+"\n");
	}
	//! [accountsummaryend]
	@Override
	public void verifyMessageAPI(String apiData) {
		System.out.println("verifyMessageAPI");
	}

	@Override
	public void verifyCompleted(boolean isSuccessful, String errorText) {
		System.out.println("verifyCompleted");
	}

	@Override
	public void verifyAndAuthMessageAPI(String apiData, String xyzChallange) {
		System.out.println("verifyAndAuthMessageAPI");
	}

	@Override
	public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {
		System.out.println("verifyAndAuthCompleted");
	}
	//! [displaygrouplist]
	@Override
	public void displayGroupList(int reqId, String groups) {
		System.out.println("Display Group List. ReqId: "+reqId+", Groups: "+groups+"\n");
	}
	//! [displaygrouplist]
	
	//! [displaygroupupdated]
	@Override
	public void displayGroupUpdated(int reqId, String contractInfo) {
		System.out.println("Display Group Updated. ReqId: "+reqId+", Contract info: "+contractInfo+"\n");
	}
	//! [displaygroupupdated]
	@Override
	public void error(Exception e) {
		logger.error("IBConnector::error() - Exception received: " + e.getMessage());
	}

	@Override
	public void error(String str) {
		logger.error("IBConnector::error() - Error string received: " + str);
	}
	//! [error]
	@Override
	public void error(int tickerId, int errorCode, String errorMsg) {
		
		logger.error("IBConnector::error() - Error. Ticker Id: " + tickerId + ", Code: " + errorCode + ", Msg: " + errorMsg + "\n");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			logger.error("IBConnector::error() - No message queue defined for ticker id " + tickerId);
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put(new IBErrorMessage(tickerId, errorCode, errorMsg ));
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::error() - received an interruptedException and have no idea what to do with it.");
		}
	}
	//! [error]
	@Override
	public void connectionClosed() {
		System.out.println("Connection closed");
	}

	//! [connectack]
	@Override
	public void connectAck() {
		if (clientSocket.isAsyncEConnect()) {
			System.out.println("Acknowledging connection");
			clientSocket.startAPI();
		}
	}
	//! [connectack]
	
	//! [positionmulti]
	@Override
	public void positionMulti(int reqId, String account, String modelCode,
			Contract contract, double pos, double avgCost) {
		System.out.println("Position Multi. Request: " + reqId + ", Account: " + account + ", ModelCode: " + modelCode + ", Symbol: " + contract.symbol() + ", SecType: " + contract.secType() + ", Currency: " + contract.currency() + ", Position: " + pos + ", Avg cost: " + avgCost + "\n");
	}
	//! [positionmulti]
	
	//! [positionmultiend]
	@Override
	public void positionMultiEnd(int reqId) {
		System.out.println("Position Multi End. Request: " + reqId + "\n");
	}
	//! [positionmultiend]
	
	//! [accountupdatemulti]
	@Override
	public void accountUpdateMulti(int reqId, String account, String modelCode,
			String key, String value, String currency) {
		System.out.println("Account Update Multi. Request: " + reqId + ", Account: " + account + ", ModelCode: " + modelCode + ", Key: " + key + ", Value: " + value + ", Currency: " + currency + "\n");
	}
	//! [accountupdatemulti]
	
	//! [accountupdatemultiend]
	@Override
	public void accountUpdateMultiEnd(int reqId) {
		System.out.println("Account Update Multi End. Request: " + reqId + "\n");
	}
	
	@Override
	public void securityDefinitionOptionalParameter(int tickerId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) 
	{
		logger.debug("IBConnector::securityDefinitionOptionalParameter - Security Definition Optional Parameter. Request: "+tickerId +", Exchange: "+exchange +", Underlying Contract Id: "+underlyingConId+", Trading Class: "+tradingClass+", Multiplier: "+multiplier+" \n");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new AvailableOptionsMessage(tickerId, exchange, underlyingConId, tradingClass, multiplier, expirations, strikes ) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::securityDefinitionOptionalParameter - InterruptedException caught in securityDefinitionOptionalParameter. Message: " + e.getMessage() );
		}
	}
	
	@Override
	public void securityDefinitionOptionalParameterEnd(int tickerId) 
	{
		logger.debug("IBConnector::securityDefinitionOptionalParameterEnd - Ending stream of available options.");
		
		MessageQueues messageQueues = this.messageQueues.get(tickerId);
		
		if ( messageQueues == null )
			return;
		
		BlockingQueue<IBMessage> messageQueue = messageQueues.getResponseMessagesQueue();
		
		if ( messageQueue == null )
		{
			return;
		}
		
		try {
			synchronized( messageQueues )
			{
				messageQueue.put( new StreamEndMessage(tickerId) );
				messageQueues.notifyAll();
			}
		}
		catch (InterruptedException e )
		{
			logger.error("IBConnector::securityDefinitionOptionalParameterEnd - InterruptedException caught in securityDefinitionOptionalParameter. Message: " + e.getMessage() );
		}
	}
	
	@Override
	public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
		for (SoftDollarTier tier : tiers) {
			System.out.print("tier: " + tier + ", ");
		}
		
		System.out.println();
	}
	
	public static IBConnector getInstance()
	{
		if ( instance == null )
			instance = new IBConnector();
		
		return instance;
	}
}