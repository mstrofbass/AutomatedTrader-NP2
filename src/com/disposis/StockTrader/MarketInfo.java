package com.disposis.StockTrader;

import com.disposis.util.Configuration;
import com.disposis.util.Logger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;

public class MarketInfo {
	
	protected static final ZoneId easternTZ = ZoneId.of("America/New_York");
	protected static final LocalTime normalMarketOpenTime = LocalTime.of(8, 30);
	protected static final LocalTime normalMarketCloseTime = LocalTime.of(16, 0);
	
	private static MarketInfo instance;

	protected Configuration conf;
	protected Logger logger;
	
	protected HashMap<String, String> exchangeMap;
	protected HashSet<LocalDate> marketHolidays;
	protected HashMap<LocalDate, ZonedDateTime> shortDays;
	
	private MarketInfo() {
		this.conf = Configuration.getInstance();
		this.logger = Logger.getInstance();
		
		this.exchangeMap = this.conf.getExchangeMap();
		this.marketHolidays = this.conf.getMarketHolidays();
		this.shortDays = this.conf.getShortDays();
	}
	
	public boolean isHoliday( LocalDate date )
	{
		return marketHolidays.contains(date);
	}
	
	public boolean isShortDay( LocalDate date )
	{
		return shortDays.containsKey(date);
	}
	
	public boolean isMarketOpen()
	{
		return isMarketOpen( ZonedDateTime.now(easternTZ) );
	}
	
	public boolean isMarketOpen( ZonedDateTime dt )
	{
		ZonedDateTime datetime = dt.withZoneSameInstant(easternTZ);
		
		ZonedDateTime marketOpen = ZonedDateTime.of(datetime.toLocalDate(), normalMarketOpenTime, easternTZ);
		ZonedDateTime marketClose = ZonedDateTime.of(datetime.toLocalDate(), normalMarketCloseTime, easternTZ);
		
		return datetime.isAfter(marketOpen) && datetime.isBefore(marketClose);
	}
	
	public boolean isTradingDay( LocalDate date )
	{
		return !isHoliday( date ) && date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
	}
	
	public ZonedDateTime getMarketOpenTime()
	{
		return getMarketOpenTime( LocalDate.now( easternTZ ) );
	}
	
	public ZonedDateTime getMarketOpenTime( LocalDate date )
	{
		return ZonedDateTime.of(date, MarketInfo.normalMarketOpenTime, easternTZ);
	}
	
	public ZonedDateTime getMarketCloseTime()
	{
		return getMarketCloseTime( LocalDate.now( easternTZ ) );
	}
	
	public ZonedDateTime getMarketCloseTime( LocalDate date )
	{
		if ( isShortDay( date ) )
			return shortDays.get(date);
		
		return ZonedDateTime.of(date, MarketInfo.normalMarketCloseTime, easternTZ);
	}
	
	public String getExchange( String symbol )
	{
		return exchangeMap.get(symbol);
	}
	
	public static MarketInfo getInstance()
	{
		if ( instance == null )
			instance = new MarketInfo();
		
		return instance;
	}

}
