package com.disposis.StockTrader.Managers;

import com.disposis.StockTrader.DataRetrievers.AsynchronousDataRetriever;
import com.disposis.StockTrader.DataRetrievers.ContractDetailsDataRetriever;
import com.disposis.StockTrader.Exceptions.DataNotLoadedException;
import com.disposis.StockTrader.Instruments.Option;
import com.disposis.StockTrader.Instruments.Stock;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.OrderType;
import com.ib.client.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OptionManager extends ContractDetailsDataRetriever {
	
	protected class OptionDataRequest extends ContractDetailsRequest
	{
		public OptionDataRequest( int requestId, BlockingQueue<AsynchronousDataRetriever.DataRequest> completedRequestsQueue, Contract contract )
		{
			super( requestId, completedRequestsQueue, contract );
		}
	};
	
	private static OptionManager instance;
	
	protected StockManager stockManager;
	protected OptionIndex optionIndex;
	
	public OptionManager() {
		super();
		
		stockManager = StockManager.getInstance();
	}
	
	@Override
	public void run() {
		optionIndex = OptionIndex.getInstance();
		super.run();
	}
	
	public Option getOption( int underlyingContractId, Types.Right optionType, int multiplier, LocalDate expirationDate, double strike )
	{
		Option option = null;
		
		try {
			option = optionIndex.getOption( underlyingContractId, optionType, multiplier, expirationDate, strike );
		}
		catch ( DataNotLoadedException e )
		{
			Stock underlyingStock = stockManager.getStockByContractId( underlyingContractId );
			
			if ( underlyingStock == null )
			{
				logger.error("OptionManager::getExpirationDates() - StockManager returned null for underlying stock with contract id ", underlyingContractId);
				return null;
			}
			
			loadOptionData( underlyingStock.getSymbol() );
			
			try {
				option = optionIndex.getOption( underlyingContractId, optionType, multiplier, expirationDate, strike );
			}
			catch ( DataNotLoadedException f )
			{
				// just gonna return null if it didn't load
			}
		}
		
		return option;
	}
	
	public Option getOptionByContractId( int contractId )
	{
		Option option = null;
		
		try {
			option = optionIndex.getByContractId(contractId);
		}
		catch ( DataNotLoadedException e )
		{
			loadOptionData(contractId);
			
			try {
				option = optionIndex.getByContractId(contractId);
			}
			catch ( DataNotLoadedException f )
			{
				// just gonna return null if it didn't load
			}
		}
		
		return option;
	}
	
	public List<Option> getOptionsForStock( Stock stock )
	{
		List<Option> options = null;
		
		try {
			options = optionIndex.getForStock(stock);
		}
		catch ( DataNotLoadedException e )
		{
			loadOptionData( stock.getSymbol() );
			
			try {
				options = optionIndex.getForStock(stock);
			}
			catch ( DataNotLoadedException f )
			{
				// just gonna return null if it didn't load
			}
		}
		
		return options;
	}
	
	public Set<LocalDate> getExpirationDates( int underlyingContractId, Types.Right optionType, int multiplier ) 
	{
		Set<LocalDate> expirationDates = null;
		
		try {
			expirationDates = optionIndex.getExpirationDates( underlyingContractId, optionType, multiplier );
		}
		catch ( DataNotLoadedException e )
		{
			Stock underlyingStock = stockManager.getStockByContractId( underlyingContractId );
			
			if ( underlyingStock == null )
			{
				logger.error("OptionManager::getExpirationDates() - StockManager returned null for underlying stock with contract id ", underlyingContractId);
				return null;
			}
			
			loadOptionData( underlyingStock.getSymbol() );
			
			try {
				expirationDates = optionIndex.getExpirationDates( underlyingContractId, optionType, multiplier );
			}
			catch ( DataNotLoadedException f )
			{
				// just gonna return null if it didn't load
			}
		}
		
		return expirationDates;
	}
	
	public Set<Double> getStrikes( int underlyingContractId, Types.Right optionType, int multiplier, LocalDate expirationDate )
	{
		Set<Double> strikes = null;
		
		try {
			strikes = optionIndex.getStrikes( underlyingContractId, optionType, multiplier, expirationDate );
		}
		catch ( DataNotLoadedException e )
		{
			Stock underlyingStock = stockManager.getStockByContractId( underlyingContractId );
			
			if ( underlyingStock == null )
			{
				logger.error("OptionManager::getStrikes() - StockManager returned null for underlying stock with contract id ", underlyingContractId);
				return null;
			}
			
			loadOptionData( underlyingStock.getSymbol() );
			
			try {
				strikes = optionIndex.getStrikes( underlyingContractId, optionType, multiplier, expirationDate );
			}
			catch ( DataNotLoadedException f )
			{
				// just gonna return null if it didn't load
			}
		}
		
		return strikes;
	}
	
	protected void loadOptionData( int contractId )
	{
		logger.debug("OptionManager::loadOptionData() - Requesting data for option with con id: %s.", contractId);
		
		Contract contract = new Contract();
		contract.conid(contractId);
		
		loadOptionData( contract );
		
		logger.debug("OptionManager::loadOptionData() - Completed request for option with con id %s.", contractId );
	}
	
	protected void loadOptionData( String underlyingSymbol )
	{
		logger.debug("OptionManager::loadOptionData() - Requesting options for stock %s.", underlyingSymbol);
		
		Contract contract = new Contract();
		
		contract.symbol(underlyingSymbol);
		contract.secType(Types.SecType.OPT.name());
		contract.currency("USD");
		contract.exchange("SMART");
		
		loadOptionData( contract );
		
		logger.debug("OptionManager::loadOptionData() - Completed request for options for stock %s.", underlyingSymbol );
	}
	
	protected void loadOptionData( Contract contract )
	{
		logger.debug("OptionManager::loadOptionData - Requesting option with contract id %s.", contract.conid());
		
		LinkedBlockingQueue<DataRequest> completedRequestsQueue = new LinkedBlockingQueue<>();
		
		this.addNewRequest(new OptionDataRequest(1, completedRequestsQueue, contract));
		
		DataRequest completedRequest = null;
		
		while ( completedRequest == null )
		{
			try {
				completedRequest = completedRequestsQueue.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		logger.debug("OptionManager::loadOptionData - Completed request for options for contract id %s.", contract.conid() );
	}
	
	@Override
	protected void handleCompletedRequest(DataRequest completedRequest) {
		
		if ( !completedRequest.hasError() && completedRequest instanceof OptionDataRequest )
		{
			OptionDataRequest odr = (OptionDataRequest) completedRequest;
			
			for ( ContractDetails cd : odr.contractDetails )
			{
				Option option = createOptionFromContractDetails( cd );
				optionIndex.addToIndex(option);
			}
		}
		
		super.handleCompletedRequest(completedRequest);
	}
	
	protected Option createOptionFromContractDetails( ContractDetails cd )
	{
		logger.trace("OptionManager::createOptionFromContractDetails() - Building open from contract details.");
		
		Option option = new Option( cd.contract().conid(), cd.contract().localSymbol(), LocalDate.parse(cd.contract().lastTradeDateOrContractMonth(), DateTimeFormatter.BASIC_ISO_DATE ), cd.contract().right(), cd.contract().strike() );
		
		option.setPrimaryExchange( cd.contract().primaryExch() );
		option.setUnderlyingSymbol(cd.contract().symbol());
		option.setExchange( cd.contract().exchange() );
		option.setCurrency( cd.contract().currency() );
		option.setExchangeSymbol( cd.contract().localSymbol() );
		option.setTradingClass( cd.contract().tradingClass() );
		option.setUnderlyingContractId( cd.underConid() );
		
		String[] orderTypeStrings = cd.orderTypes().split(",");
		Set<OrderType> orderTypes = new HashSet<>();
		
		for ( String orderTypeString : orderTypeStrings )
		{
			OrderType orderType = OrderType.get(orderTypeString.trim());
			
			if ( orderType == OrderType.None )
				continue;
			
			orderTypes.add( orderType );
		}
		
		option.setOrderTypes(orderTypes);
		
		return option;
	}
	
	public static OptionManager getInstance()
	{
		if (instance == null)
			instance = new OptionManager();
		
		return instance;
	}
}
