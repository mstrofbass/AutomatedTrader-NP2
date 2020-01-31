package com.disposis.StockTrader.APIConnector;

public abstract class IBMessage {
	private final int tickerId;
	
	public IBMessage( int tickerId )
	{
		this.tickerId = tickerId;
	}
	
	public int getTickerId()
	{
		return tickerId;
	}

	@Override
	public String toString() {
		return "IBMessage [tickerId=" + tickerId + "]";
	}
	
	
}