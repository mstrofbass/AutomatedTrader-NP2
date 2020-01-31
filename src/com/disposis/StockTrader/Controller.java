package com.disposis.StockTrader;

import com.disposis.StockTrader.APIConnector.IBConnector;
import com.disposis.StockTrader.APIConnector.IBErrorMessage;
import com.disposis.StockTrader.APIConnector.IBMessage;
import com.disposis.StockTrader.DataRetrievers.MessageQueues;
import com.disposis.StockTrader.Instruments.Stock;
import com.disposis.StockTrader.Managers.OptionManager;
import com.disposis.StockTrader.Managers.StockManager;
import com.disposis.StockTrader.Managers.TickDataManager;
import com.disposis.StockTrader.Strategies.ReboundStrategy;
import com.disposis.StockTrader.Strategies.Strategy;
import com.disposis.util.Configuration;
import com.disposis.util.Logger;
import com.disposis.util.TransactionLogger;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Controller {

	protected static final ZoneId easternTZ = ZoneId.of("America/New_York");
	private static Controller instance;
	
	protected volatile boolean run = true;
	
	protected Configuration conf;
	protected Logger logger;
	protected TransactionLogger transLogger;
	protected IBConnector connector;
	protected MarketInfo marketInfo;
	
	protected Strategy strategy;
	
	LinkedBlockingQueue<IBMessage> generalMessageQueue = new LinkedBlockingQueue<>();
	MessageQueues messageQueues;
	
	protected List<String> symbols;
	protected List<Stock> stocks;
	
	protected StockManager stockManager;
	protected Thread stockManagerThread;
	
	protected OptionManager optionManager;
	protected Thread optionManagerThread;

	protected TickDataManager tickDataManager;
	protected Thread tickDataManagerThread;

	public Controller() {
		this.conf = Configuration.getInstance();
		this.logger = Logger.getInstance();
		this.transLogger = TransactionLogger.getInstance();
		this.marketInfo = MarketInfo.getInstance();
		
		this.symbols = this.conf.getSymbols();
	}
	
	protected void init() throws Exception
	{
		logger.trace( "Controller::init() - Entering init method." );
		
		logger.console( "Controller::init() - Initializing IBConnector." );
		
		connector = IBConnector.getInstance();
		connector.setMessageQueue(-1, new MessageQueues());
		
		logger.console( "Controller::init() - Connecting to endpoint." );
		
		boolean connected = false;
		
		connector.connect();
		
		try {
			
			IBMessage connectionMessage;
			IBErrorMessage errorMessage; 
			
			while ( !connected )
			{
				logger.trace("Polling connector for connection");
				connectionMessage = generalMessageQueue.poll( 1000, TimeUnit.MILLISECONDS );
				
				if ( connector.getCurrentOrderId() == -1 && connectionMessage instanceof IBErrorMessage )
				{
					errorMessage = (IBErrorMessage) connectionMessage;
					
					switch ( errorMessage.getCode() )
					{
						case IBErrorMessage.CLIENT_ALREADY_CONNECTED:
							connected = true;
							logger.error("Controller::init() - Error message received while trying to connect to the server. Error code: %d Error Message: %s", errorMessage.getCode(), errorMessage.getMessage() );
						break;
							
						case IBErrorMessage.CLIENT_CONNECTION_ATTEMPT_FAILED:
						case IBErrorMessage.CLIENT_GATEWAY_OUT_OF_DATE:
						case IBErrorMessage.CLIENT_NOT_CONNECTED:
							logger.error("Controller::init() - Error message received while trying to connect to the server. Error code: %d Error Message: %s", errorMessage.getCode(), errorMessage.getMessage() );
							throw new Exception("Couldn't connect to server.");
					}
				}
				else if ( connector.getCurrentOrderId() > -1 )
				{
					logger.trace("Controller::init() - Connected to server.");
					connected = true;
				}
			}
			
		} catch (InterruptedException e1) {

			e1.printStackTrace();
		}
		
		logger.console( "Controller::init() - Connected. Initializing connector." );
		
		connector.init();
		
		logger.console( "Controller::init() - Connector initialized." );
		logger.console( "Controller::init() - Initializing threads." );
		
		logger.trace( "Controller::init() - Initializing stock manager thread." );
		
		stockManager = StockManager.getInstance();
		
		stockManagerThread = new Thread(stockManager);
		stockManagerThread.start();
		
		logger.trace( "Controller::init() - stock manager thread initialized." );
		
		logger.trace( "Controller::init() - Initializing tick data manager thread." );
		
		tickDataManager = TickDataManager.getInstance();
		
		tickDataManagerThread = new Thread(tickDataManager);
		tickDataManagerThread.start();
		
		logger.trace( "Controller::init() - tick data manager thread initialized." );
		logger.trace( "Controller::init() - Initializing option manager retriever thread." );
		
		optionManager = OptionManager.getInstance();
		
		optionManagerThread = new Thread(optionManager);
		optionManagerThread.start();
		
		logger.trace( "Controller::init() - Option manager thread initialized." );
		
		logger.console( "Controller::init() - Threads initialized." );
		
		logger.console( "Controller::init() - Initializing strategy." );
		
		this.strategy = new ReboundStrategy();
		
		logger.console( "Controller::init() - Strategy initialized." );
	}
	
	protected void initData() throws InterruptedException, IOException
	{
		logger.trace( "Controller::initData() - Entering initData()." );
		logger.console( "Controller::initData() - Loading stocks." );
		
		// since the rest of the data loading relies on the stock data being available, this is done synchronously
		stocks = loadStocks( symbols );
		
		logger.console( "Controller::initData() - Stocks loaded." );
		logger.console( "Controller::initData() - Beginning streaming quotes and loading initial data requests." );
		
		for ( Stock stock : stocks )
		{	
			logger.trace("Controller::initData() - Sending request for tick data for contract id %s and symbol %s", stock.getContractId(), stock.getSymbol() );
			tickDataManager.getStreamingTickData(stock);
		}
		
		logger.console( "Controller::initData() - Streaming quote requests and initial data requests done." );
		logger.trace( "Controller::initData() - Exiting initData()." );
	}
	
	public void run() throws Exception
	{
		try {
			init();
			initData();
			runInternal();
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			if ( connector != null )
				connector.disconnect();
		}
	}

	protected void runInternal() throws Exception
	{
		logger.trace("Controller::runInternal() - Entering runInternal();");
		
		ZonedDateTime currentDateTime;
		ZonedDateTime nextDay;
		long sleepTime;
		
		// open period
		ZonedDateTime openAnalysisTime;
		// post open period
		ZonedDateTime marketCloseTime  = marketInfo.getMarketCloseTime();
		
		boolean openAnalysisComplete = false;
		
		logger.console("Controller::runInternal() - Beginning run loop.");
		
		while ( run )
		{
			currentDateTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
			openAnalysisTime = marketCloseTime.minusMinutes(30);
			
			if ( !marketInfo.isTradingDay( currentDateTime.toLocalDate() ) ) // market not open
			{
				logger.console( "Controller::runInternal() - Market not open today; sleeping until the next day." );
				nextDay = currentDateTime.plusDays(1).toLocalDate().atStartOfDay(ZoneId.of("America/New_York")).plusMinutes(15);
				
				sleepTime = ChronoUnit.MILLIS.between(currentDateTime, nextDay);
				
				logger.debug("Controller::runInternal() - Current time: %s nextDay: %s sleepTime: %s", currentDateTime, nextDay, sleepTime );
				
				Thread.sleep(sleepTime);
			}
			else if ( !marketInfo.isMarketOpen() )
			{
				logger.console( "Controller::runInternal() - Market not open yet; scheduling sleeping until open time." );
				sleepTime = ChronoUnit.MILLIS.between(currentDateTime, marketInfo.getMarketOpenTime() );
				
				logger.debug("Controller::runInternal() - Current time: %s sleepTime: %s", currentDateTime, sleepTime );
				
				Thread.sleep(sleepTime);
			}
			else
			{
				for ( Stock stock : stocks )
				{
					try {
						logger.trace("Controller::runInternal() - Passing stock %s to strategy for close positions analysis.", stock.getSymbol() );
						strategy.analyze(stock, false, true);
					}
					catch (Exception e)
					{
						logger.error( "Controller::runInternal() - Exception caught while analyzing stock %s.", e.toString() );
						logger.console( "Controller::runInternal() - Exception caught while analyzing stock %s.", e.toString() );
						e.printStackTrace();
					}
				}
				
				if ( !openAnalysisComplete && ( currentDateTime.isAfter(openAnalysisTime) && currentDateTime.isBefore(marketCloseTime.minusMinutes(5) ) ) )
				{
					logger.debug("Controller::runInternal() - Performing open analysis.");
					
					for ( Stock stock : stocks )
					{
						try {
							logger.trace("Controller::runInternal() - Passing stock %s to strategy for open positions analysis.", stock.getSymbol() );
							strategy.analyze(stock, true, false);
						}
						catch (Exception e)
						{
							logger.error( "Controller::runInternal() - Exception caught while analyzing stock %s.", e.toString() );
							logger.console( "Controller::runInternal() - Exception caught while analyzing stock %s.", e.toString() );
							e.printStackTrace();
						}
					}
					
					openAnalysisComplete = true;
				}
				
				Thread.sleep(10000);
			}
		}
    		
        logger.console( "Controller::runInternal() - Done." );
	}
	
	public void shutdown()
	{
		logger.console("Controller::shutdown() - Shutting everything down.");
				
		tickDataManager.shutdown();
		tickDataManagerThread.interrupt();
		
		stockManager.shutdown();
		stockManagerThread.interrupt();

		optionManager.shutdown();
		optionManagerThread.interrupt();
		
		logger.console( "Controller::shutdown() - Disconnecting client." );
        
		connector.disconnect();
		
		run = false;
		
		logger.console( "Controller::shutdown() - Done shutting everything down." );
	}
	
	protected List<Stock> loadStocks( List<String> symbols )
	{
		ArrayList<Stock> loadedStocks = new ArrayList<>();
		
		for ( String symbol : symbols )
		{
			loadedStocks.add( stockManager.getStockBySymbolAndPrimaryExchange(symbol, conf.getPrimaryExchange(symbol)) );
		}
		
		return loadedStocks;
	}
}