//package com.disposis.StockTrader.DataRetrievers;
//
//import com.disposis.StockTrader.Instruments.Option;
//import com.disposis.StockTrader.Managers.OptionIndex;
//import com.disposis.StockTrader.Instruments.Stock;
//import com.ib.client.Contract;
//import com.ib.client.ContractDetails;
//import com.ib.client.OrderType;
//import com.ib.client.Types.SecType;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class OptionDetailsDataRetriever extends ContractDetailsDataRetriever {
//	
//	public class OptionDetailsRequest extends ContractDetailsRequest
//	{
//		public OptionDetailsRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue, Option option )
//		{
//			super( requestId, completedRequestsQueue, option.getContract() );
//		}
//		
//		public OptionDetailsRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue, Contract contract )
//		{
//			super( requestId, completedRequestsQueue, contract );
//		}
//	};
//	
//	protected static OptionDetailsDataRetriever instance;
//	protected OptionIndex optionIndex;
//
//	private OptionDetailsDataRetriever() 
//	{ 
//		super();
//	}
//	
//	@Override
//	public void run() {
//		optionIndex = OptionIndex.getInstance();
//		super.run();
//	}
//	
//	@Override
//	protected void handleCompletedRequest(DataRequest completedRequest) {
//		
//		if ( !completedRequest.hasError() && completedRequest instanceof OptionDetailsRequest )
//		{
//			OptionDetailsRequest odr = (OptionDetailsRequest) completedRequest;
//			
//			for ( ContractDetails cd : odr.contractDetails )
//			{
//				Option option = createOptionFromContractDetails( cd );
//				optionIndex.addToIndex(option);
//			}
//		}
//		
//		super.handleCompletedRequest(completedRequest);
//	}
//	
//	protected Option createOptionFromContractDetails( ContractDetails cd )
//	{
//		logger.trace("OptionDetailsDataRetriever::createOptionFromContractDetails() - Building open from contract details.");
//		
//		Option option = new Option( cd.contract().conid(), cd.contract().localSymbol(), LocalDate.parse(cd.contract().lastTradeDateOrContractMonth(), DateTimeFormatter.BASIC_ISO_DATE ), cd.contract().right(), cd.contract().strike() );
//		
//		option.setPrimaryExchange( cd.contract().primaryExch() );
//		option.setUnderlyingSymbol(cd.contract().symbol());
//		option.setExchange( cd.contract().exchange() );
//		option.setCurrency( cd.contract().currency() );
//		option.setExchangeSymbol( cd.contract().localSymbol() );
//		option.setTradingClass( cd.contract().tradingClass() );
//		option.setUnderlyingContractId( cd.underConid() );
//		
//		String[] orderTypeStrings = cd.orderTypes().split(",");
//		Set<OrderType> orderTypes = new HashSet<OrderType>();
//		
//		for ( String orderTypeString : orderTypeStrings )
//		{
//			OrderType orderType = OrderType.get(orderTypeString.trim());
//			
//			if ( orderType == OrderType.None )
//				continue;
//			
//			orderTypes.add( orderType );
//		}
//		
//		option.setOrderTypes(orderTypes);
//		
//		return option;
//	}
//	
//	public void loadOptionByContractId( int contractId )
//	{
//		logger.debug("OptionDetailsDataRetriever::getOptions() - Requesting option with contract id %s.", contractId);
//		
//		LinkedBlockingQueue<DataRequest> completedRequestsQueue = new LinkedBlockingQueue<DataRequest>();
//		
//		Contract contract = new Contract();
//		contract.conid(contractId);
//		
//		this.addNewRequest(new OptionDetailsRequest(1, completedRequestsQueue, contract));
//		
//		DataRequest completedRequest = null;
//		
//		while ( completedRequest == null )
//		{
//			try {
//				completedRequest = completedRequestsQueue.take();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		logger.debug("Completed request for options for contract id %s.", contractId );
//	}
//	
//	public void loadOptionForStock( Stock stock )
//	{
//		logger.debug("OptionDetailsDataRetriever::getOptions() - Requesting options for stock %s.", stock.getSymbol());
//		
//		LinkedBlockingQueue<DataRequest> completedRequestsQueue = new LinkedBlockingQueue<DataRequest>();
//		
//		Contract contract = new Contract();
//		
//		contract.symbol(stock.getSymbol());
//		contract.secType(SecType.OPT.name());
//		contract.currency("USD");
//		contract.exchange("SMART");
//		
//		this.addNewRequest(new OptionDetailsRequest(1, completedRequestsQueue, contract));
//		
//		DataRequest completedRequest = null;
//		
//		while ( completedRequest == null )
//		{
//			try {
//				completedRequest = completedRequestsQueue.take();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		logger.debug("Completed request for options for stock %s.", stock.getSymbol());
//	}
//	
//	public static OptionDetailsDataRetriever getInstance()
//	{
//		if ( instance == null )
//			instance = new OptionDetailsDataRetriever();
//		
//		return instance;
//	}
//}