package com.disposis.StockTrader.APIConnector;

public final class OrderStatusMessage extends IBMessage {

	private final String status;
	private final double filled;
	private final double remaining;
	private final double avgFillPrice;
	private final int permId;
	private final int parentId;
	private final double lastFillPrice;
	private final int clientId;
	private final String whyHeld;
	
	public OrderStatusMessage( int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld )
	{
		super(orderId);
		
		this.status = status;
		this.filled = filled;
		this.remaining = remaining;
		this.avgFillPrice = avgFillPrice;
		this.permId = permId;
		this.parentId = parentId;
		this.lastFillPrice = lastFillPrice;
		this.clientId = clientId;
		this.whyHeld = whyHeld;
	}

	public int getOrderId()
	{
		return getTickerId();
	}
	
	public String getStatus() {
		return status;
	}

	public double getFilled() {
		return filled;
	}

	public double getRemaining() {
		return remaining;
	}

	public double getAvgFillPrice() {
		return avgFillPrice;
	}

	public int getPermId() {
		return permId;
	}

	public int getParentId() {
		return parentId;
	}

	public double getLastFillPrice() {
		return lastFillPrice;
	}

	public int getClientId() {
		return clientId;
	}

	public String getWhyHeld() {
		return whyHeld;
	}
}