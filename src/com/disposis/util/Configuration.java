package com.disposis.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Configuration {
	
	protected static final int MODE_TEST = 0;
	protected static final int MODE_PROD = 1;
	
	protected static Configuration instance;
	
	protected Config conf;
	protected List<String> symbols;
	protected String stockDataFilePath;
	
	protected String logFilePath;
	protected int logLevel;
	
	protected String transLogFilePath;
	
	protected int mode;
	
	protected HashMap<String, String> exchangeMap = new HashMap<String, String>();
	protected HashSet<LocalDate> marketHolidays = new HashSet<LocalDate>();
	protected HashMap<LocalDate, ZonedDateTime> shortDays = new HashMap<LocalDate, ZonedDateTime>();
	
	public Configuration() throws Exception
	{
		conf = ConfigFactory.load();
		
		if ( !conf.hasPath("app.symbols") )
			throw new Exception("Symbols not set in config file.");
		
		if ( !conf.hasPath("app.mode") )
			throw new Exception("Mode not set in config file.");
		
		if ( !conf.hasPath("app.stockDataFilePath") )
			throw new Exception("Stock data file path not set in config file.");
		
		if ( !conf.hasPath("app.marketInfo.holidays") )
			throw new Exception("Market holidays not set in config file. ");
		
		if ( !conf.hasPath("app.marketInfo.shortDays") )
			throw new Exception("Market short days not set in config file.");
		
		if ( !conf.hasPath("logger.dir") )
			throw new Exception("Log directory not set in config file.");
		
		if ( !conf.hasPath("logger.filename") )
			throw new Exception("Log filename not set in config file.");
		
		if ( !conf.hasPath("transLogger.dir") )
			throw new Exception("Transaction log directory not set in config file.");
		
		if ( !conf.hasPath("transLogger.filename") )
			throw new Exception("Transaction log filename not set in config file.");
		
		symbols = conf.getStringList("app.symbols");
		stockDataFilePath = conf.getString("app.stockDataFilePath");
		
		String logFileDir = conf.getString("logger.dir");
		String logFilename = conf.getString("logger.filename");
		
		logFilePath = Paths.get(logFileDir, logFilename).toString();
		
		String transLogFileDir = conf.getString("transLogger.dir");
		String transLogFilename = conf.getString("transLogger.filename");
		
		transLogFilePath = Paths.get(transLogFileDir, transLogFilename).toString();
		
		String logLevelSetting = conf.hasPath("logger.logLevel") ? conf.getString("logger.logLevel").toUpperCase() : "ERROR";
		
		switch ( conf.getString("app.mode") )
		{
			
			
			case "PROD":
				this.mode = Configuration.MODE_PROD;
			break;
			
			case "TEST":
			default:
				this.mode = Configuration.MODE_TEST;
			break;
		}
		
		switch ( logLevelSetting )
		{
			case "TRACE":
				this.logLevel = Logger.LOG_LEVEL_TRACE;
			break;
			
			case "DEBUG":
				this.logLevel = Logger.LOG_LEVEL_DEBUG;
			break;
				
			case "MESSAGE":
				this.logLevel = Logger.LOG_LEVEL_MESSAGE;
			break;
			
			case "ERROR":
			default:
				this.logLevel = Logger.LOG_LEVEL_ERROR;
			break;
		}
		
		loadExchangeMap();
		loadHolidays();
		loadShortDays();
	}
	
	protected void loadExchangeMap()
	{
		for ( Map.Entry<String, ConfigValue> entry : conf.getObject("app.exchangeMap").entrySet() )
		{
			exchangeMap.put(entry.getKey(), (String) entry.getValue().unwrapped() );
		}
	}
	
	protected void loadHolidays()
	{
		List<String> stringDates = conf.getStringList("app.marketInfo.holidays");
		
		for ( String stringDate : stringDates )
		{
			marketHolidays.add( LocalDate.parse(stringDate) );
		}
	}
	
	protected void loadShortDays()
	{
		String dateString;
		String timeString;
		
		LocalDate date;
		ZonedDateTime dateTime;
		
		for ( Map.Entry<String, ConfigValue> entry : conf.getObject("app.marketInfo.shortDays").entrySet() )
		{
			dateString = entry.getKey();
			timeString = (String) entry.getValue().unwrapped();
			
			date = LocalDate.parse(dateString);
			dateTime = ZonedDateTime.of(date, LocalTime.parse(timeString), ZoneId.of("America/Chicago"));
			
			shortDays.put(date, dateTime );
		}
	}
	
	public HashMap<String, String> getExchangeMap() {
		return exchangeMap;
	}

	protected void setExchangeMap(HashMap<String, String> exchangeMap) {
		this.exchangeMap = exchangeMap;
	}
	
	public String getPrimaryExchange( String symbol )
	{
		return exchangeMap.get(symbol);
	}
	
	public List<String> getSymbols() {
		return symbols;
	}

	public void setSymbols(List<String> symbols) {
		this.symbols = symbols;
	}

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public String getTransLogFilePath() {
		return transLogFilePath;
	}

	public HashSet<LocalDate> getMarketHolidays() {
		return marketHolidays;
	}

	public void setMarketHolidays(HashSet<LocalDate> marketHolidays) {
		this.marketHolidays = marketHolidays;
	}

	public HashMap<LocalDate, ZonedDateTime> getShortDays() {
		return shortDays;
	}

	public void setShortDays(HashMap<LocalDate, ZonedDateTime> shortDays) {
		this.shortDays = shortDays;
	}

	public String getStockDataFilePath() {
		return stockDataFilePath;
	}

	public void setStockDataFilePath(String stockDataFilePath) {
		this.stockDataFilePath = stockDataFilePath;
	}

	public static Configuration getInstance()
	{
		if ( Configuration.instance == null )
		{
			try {
				Configuration.instance = new Configuration();
			}
			catch (Exception e) {
				System.out.println( String.format("Configuration::getInstance() - Exception thrown while trying to instantiate a new configuration instance. Msg: %s", e.getMessage()));
				return null;
			}
		}
		
		return Configuration.instance;
	}
	
}
