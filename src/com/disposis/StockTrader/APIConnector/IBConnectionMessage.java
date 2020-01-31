package com.disposis.StockTrader.APIConnector;

public final class IBConnectionMessage extends IBMessage {
	
	private final boolean isConnected = false;
	
	public IBConnectionMessage( int tickerId, boolean isConnected )
	{
		super(tickerId);
	}
	
	public boolean isConnected()
	{
		return isConnected;
	}
}