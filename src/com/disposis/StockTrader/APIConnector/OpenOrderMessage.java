package com.disposis.StockTrader.APIConnector;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public final class OpenOrderMessage extends IBMessage {

	protected final Contract contract;
	protected final Order order;
	protected final OrderState orderState;
	
	public OpenOrderMessage( int orderId, Contract contract, Order order, OrderState orderState )
	{
		super(orderId);
		
		this.contract = contract;
		this.order = order;
		this.orderState = orderState;
	}
	
	public int getOrderId()
	{
		return this.getTickerId();
	}

	public Contract getContract() {
		return contract;
	}

	public Order getOrder() {
		return order;
	}

	public OrderState getOrderState() {
		return orderState;
	}
}