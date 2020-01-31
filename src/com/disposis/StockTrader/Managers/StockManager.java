package com.disposis.StockTrader.Managers;

import com.disposis.StockTrader.DataRetrievers.AsynchronousDataRetriever;
import com.disposis.StockTrader.DataRetrievers.ContractDetailsDataRetriever;
import com.disposis.StockTrader.Instruments.Stock;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.OrderType;
import com.ib.client.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StockManager extends ContractDetailsDataRetriever {
	
	protected class StockDataRequest extends ContractDetailsRequest
	{
		public StockDataRequest( int requestId, BlockingQueue<AsynchronousDataRetriever.DataRequest> completedRequestsQueue, Contract contract )
		{
			super( requestId, completedRequestsQueue, contract );
		}
	};
	
	private static StockManager instance;
	
	protected BlockingQueue<AsynchronousDataRetriever.DataRequest> completedRequestsQueue = new LinkedBlockingQueue<>();
	
	protected Map<Integer, Stock> stocksById = new HashMap<>();
	protected Map<String, Stock> stocksBySymbolAndExchange = new HashMap<>();
	
	public StockManager() {
		super();
	}
	
	public Stock getStockByContractId( int contractId )
	{
		if ( !stocksById.containsKey(contractId))
			loadStockData(contractId);
		
		return stocksById.get(contractId);
	}
	
	public Stock getStockBySymbolAndPrimaryExchange( String symbol, String primaryExchange )
	{
		String key = StockManager.generateKey(symbol, primaryExchange);
		
		if ( !stocksBySymbolAndExchange.containsKey(key))
			loadStockData(symbol, primaryExchange);
		
		return stocksBySymbolAndExchange.get(key);
	}
	
	protected void loadStockData( int contractId )
	{
		logger.debug("StockManager::loadStockData() - Requesting data for stock con id: %s.", contractId);
		
		Contract contract = new Contract();
		contract.conid(contractId);
		
		loadStockData( contract );
		
		logger.debug("StockManager::loadStockData() - Completed request for stock con id %s.", contractId );
	}
	
	protected void loadStockData( String symbol )
	{
		logger.debug("StockManager::loadStockData() - Requesting data for stock: %s.", symbol);
		
		Contract contract = new Contract();
		
		contract.symbol(symbol);
		contract.secType(Types.SecType.STK.name());
		contract.currency("USD");
		contract.exchange("SMART");
		
		loadStockData( contract );
		
		logger.debug("StockManager::loadStockData() - Completed request for stock %s.", symbol );
	}
	
	protected void loadStockData( String symbol, String primaryExchange )
	{
		logger.debug("StockManager::loadStockData() - Requesting data for stock: %s with primary exchange %s.", symbol, primaryExchange);
		
		Contract contract = new Contract();
		
		contract.symbol(symbol);
		contract.secType(Types.SecType.STK.name());
		contract.primaryExch(primaryExchange);
		contract.currency("USD");
		contract.exchange("SMART");
		
		loadStockData( contract );
		
		logger.debug("StockManager::loadStockData() - Completed request for stock %s with primary exchange %s.", symbol, primaryExchange );
	}
	
	protected void loadStockData( Contract contract )
	{
		logger.debug("StockManager::loadStockData() - Requesting data for stock: %s.", contract.toString());
		
		LinkedBlockingQueue<DataRequest> crQueue = new LinkedBlockingQueue<>();
		
		this.addNewRequest(new StockDataRequest(1, crQueue, contract));
		
		DataRequest completedRequest = null;
		
		while ( completedRequest == null )
		{
			try {
				completedRequest = crQueue.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		logger.debug("StockManager::loadStockData() - Completed request for stock: %s.", contract.toString() );
	}
	
	@Override
	protected void handleCompletedRequest(DataRequest completedRequest) {
		
		if ( !completedRequest.hasError() && completedRequest instanceof StockDataRequest )
		{
			StockDataRequest odr = (StockDataRequest) completedRequest;
			
			for ( ContractDetails cd : odr.contractDetails )
			{
				Stock stock = createStockFromContractDetails( cd );
				
				stocksById.put(stock.getContractId(), stock);
				stocksBySymbolAndExchange.put( StockManager.generateKey(stock.getSymbol(), stock.getPrimaryExchange()), stock);
			}
		}
		
		super.handleCompletedRequest(completedRequest);
	}
	
	protected Stock createStockFromContractDetails( ContractDetails cd )
	{
		logger.trace("StockManager::createStockFromContractDetails() - Building stock from contract details.");
		
		Stock stock = new Stock( cd.contract().conid(), cd.contract().symbol() );

		stock.setPrimaryExchange( cd.contract().primaryExch() );
		stock.setExchange( cd.contract().exchange() );
		stock.setCurrency( cd.contract().currency() );
		stock.setExchangeSymbol( cd.contract().localSymbol() );
		stock.setTradingClass( cd.contract().tradingClass() );
		stock.setLongName( cd.longName() );
		stock.setCategory( cd.category() );
		stock.setSubcategory( cd.subcategory() );

		String[] orderTypeStrings = cd.orderTypes().split(",");
		Set<OrderType> orderTypes = new HashSet<>();

		for ( String orderTypeString : orderTypeStrings )
		{
			OrderType orderType = OrderType.get(orderTypeString.trim());

			if ( orderType == OrderType.None )
				continue;

			orderTypes.add( orderType );
		}

		stock.setOrderTypes(orderTypes);

		String[] industryStrings = cd.industry().split(",");
		Set<String> industries = new HashSet<>();

		for ( String industryString : industryStrings )
		{
			String industry = industryString.trim();

			if ( industry.isEmpty() )
				continue;

			industries.add( industry );
		}

		stock.setIndustries(industries);

		return stock;
	}
	
	protected static String generateKey( String symbol, String primaryExchange )
	{
		return symbol.toLowerCase() + "-" + primaryExchange.toLowerCase();
	}
	
	public static StockManager getInstance()
	{
		if (instance == null)
			instance = new StockManager();
		
		return instance;
	}
}
