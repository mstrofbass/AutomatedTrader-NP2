package com.disposis.StockTrader.APIConnector;

import java.util.Set;

public final class AvailableOptionsMessage extends IBMessage {

	private final String exchange;
	private final int underlyingContractId;
	private final String tradingClass;
	private final String multiplier;
	private final Set<String> expirationDates;
	private final Set<Double> strikes;
	
	public AvailableOptionsMessage( int tickerId, String exchange, int underlyingContractId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes )
	{
		super(tickerId);
		
		this.exchange = exchange;
		this.underlyingContractId = underlyingContractId;
		this.tradingClass = tradingClass;
		this.multiplier = multiplier;
		this.expirationDates = expirations;
		this.strikes = strikes;
	}

	public String getExchange() {
		return exchange;
	}

	public int getUnderlyingContractId() {
		return underlyingContractId;
	}

	public String getTradingClass() {
		return tradingClass;
	}

	public String getMultiplier() {
		return multiplier;
	}

	public Set<String> getExpirationDates() {
		return expirationDates;
	}

	public Set<Double> getStrikes() {
		return strikes;
	}
}