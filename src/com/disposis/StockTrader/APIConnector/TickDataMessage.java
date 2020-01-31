package com.disposis.StockTrader.APIConnector;

public final class TickDataMessage extends IBMessage {

	private final int field;
	private final double price;
	private final boolean autoExecutable;
	
	public TickDataMessage( int tickerId, int field, double price, boolean autoExecutable )
	{
		super(tickerId);
		
		this.field = field;
		this.price = price;
		this.autoExecutable = autoExecutable;
	}

	public int getField() {
		return field;
	}

	public double getPrice() {
		return price;
	}

	public boolean isAutoExecutable() {
		return autoExecutable;
	}
}