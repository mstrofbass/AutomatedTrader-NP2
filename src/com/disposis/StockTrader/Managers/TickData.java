package com.disposis.StockTrader.Managers;

public class TickData implements Cloneable {
	
	public static final double DEFAULT_DOUBLE = -2.0;
	
	private double lastTick = -2.0;
	private double previousClose = -2.0;
	
	public TickData()
	{
		super();
	}
	
	public boolean hasLastTick() {
		return lastTick != -1 && lastTick != -2;
	}

	public double getLastTick() {
		return lastTick;
	}

	public void setLastTick(double lastTick) {
		this.lastTick = lastTick;
	}
	
	public boolean hasPreviousClose() {
		return previousClose != -1 && previousClose != -2;
	}

	public double getPreviousClose() {
		return previousClose;
	}

	public void setPreviousClose(double previousClose) {
		this.previousClose = previousClose;
	}
	
	public boolean isComplete()
	{
		return lastTick != -2.0 && previousClose != -2.0;
	}

	@Override
	public String toString() {
		return "TickData [lastTick=" + lastTick + ", previousClose=" + previousClose + "]";
	}

	protected TickData clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return (TickData) super.clone();
	}
	
	
}
