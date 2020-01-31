package com.disposis.StockTrader.Instruments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ib.client.Contract;
import com.ib.client.OrderType;
import com.ib.client.Types.SecType;

public abstract class TradeableInstrument implements Contractable {
	
	protected int contractId;
	protected String symbol;
	
	protected SecType securityType;
	protected String primaryExchange;
	protected String exchange; 
	protected String currency;
	protected String exchangeSymbol;
	protected String tradingClass;
	
	protected Set<OrderType> orderTypes = new HashSet<OrderType>(); 
	
	protected List<Position> openPositions = new ArrayList<Position>();
	protected List<Position> closedPositions = new ArrayList<Position>();
	
	public TradeableInstrument() {}
	
	protected TradeableInstrument( int contractId ) {
		this.contractId = contractId;
	}
	
	protected TradeableInstrument( int contractId, String symbol ) {
		this.contractId = contractId;
		this.symbol = symbol;
	}
	
	public int getContractId() {
		return contractId;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public SecType getSecurityType() {
		return securityType;
	}
	
	public String getPrimaryExchange() {
		return primaryExchange;
	}
	
	public void setPrimaryExchange(String primaryExchange) {
		this.primaryExchange = primaryExchange;
	}
	
	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public String getExchangeSymbol() {
		return exchangeSymbol;
	}

	public void setExchangeSymbol(String exchangeSymbol) {
		this.exchangeSymbol = exchangeSymbol;
	}

	public String getTradingClass() {
		return tradingClass;
	}

	public void setTradingClass(String tradingClass) {
		this.tradingClass = tradingClass;
	}
	
	public boolean hasOrderType(OrderType orderType)
	{
		return orderTypes.contains(orderType);
	}

	public Set<OrderType> getOrderTypes() {
		return orderTypes;
	}

	public void setOrderTypes(Set<OrderType> orderTypes) {
		this.orderTypes = orderTypes;
	}
	
	public void addOrderType( OrderType orderType )
	{
		this.orderTypes.add(orderType);
	}
	
	public boolean hasOpenPosition()
	{
		return openPositions.size() > 0;
	}
	
	public List<Position> getOpenPositions() {
		return openPositions;
	}

	public void setOpenPositions(List<Position> openPositions) {
		this.openPositions = openPositions;
	}
	
	public void addOpenPosition( Position position )
	{
		this.openPositions.add(position);
	}
	
	public void closePosition( Position position )
	{
		if ( !this.openPositions.contains(position) )
			return;
		
		this.openPositions.remove(position);
		this.closedPositions.add( position );
	}

	@Override
	public Contract getContract()
	{
		Contract contract = new Contract();
		contract.conid( contractId );
		
		return contract;
	}
}
