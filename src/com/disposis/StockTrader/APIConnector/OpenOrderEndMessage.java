package com.disposis.StockTrader.APIConnector;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public final class OpenOrderEndMessage extends IBMessage {
	
	public OpenOrderEndMessage( int orderId, Contract contract, Order order, OrderState orderState )
	{
		super(orderId);
	}
	
	public int getOrderId()
	{
		return this.getTickerId();
	}
}