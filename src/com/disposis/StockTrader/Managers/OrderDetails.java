package com.disposis.StockTrader.Managers;

import java.time.LocalDateTime;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class OrderDetails {
	
	protected final int orderId;
	protected final Contract contract;
	protected final Order order;
	protected final LocalDateTime placedTimestamp;
	protected LocalDateTime completedTimestamp;
	
	protected OrderStatus orderStatus = OrderStatus.OPEN;
	
	protected OrderState orderDetails;
	
	protected double filledQty = 0;
	protected double totalQty = -1;
	protected double avgPrice = -1;
	
	public OrderDetails(int orderId, Contract contract, Order order, LocalDateTime placedTimestamp )
	{
		this.orderId = orderId;
		this.contract = contract;
		this.order = order;
		this.totalQty = order.totalQuantity();
		this.placedTimestamp = placedTimestamp;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public OrderState getOrderDetails() {
		return orderDetails;
	}

	public void setOrderDetails(OrderState orderDetails) {
		this.orderDetails = orderDetails;
	}

	public double getFilledQty() {
		return filledQty;
	}

	public void setFilledQty(double filledQty) {
		this.filledQty = filledQty;
	}
	
	public double getTotalQty() {
		return totalQty;
	}

	public void setTotalQty(double totalQty) {
		this.totalQty = totalQty;
	}
	
	public double getRemainingQty()
	{
		return this.totalQty - this.filledQty;
	}

	public double getAvgPrice() {
		return avgPrice;
	}

	public void setAvgPrice(double avgPrice) {
		this.avgPrice = avgPrice;
	}

	public int getOrderId() {
		return orderId;
	}

	public Contract getContract() {
		return contract;
	}

	public Order getOrder() {
		return order;
	}

	public LocalDateTime getCompletedTimestamp() {
		return completedTimestamp;
	}

	public void setCompletedTimestamp(LocalDateTime completedTimestamp) {
		this.completedTimestamp = completedTimestamp;
	}

	public LocalDateTime getPlacedTimestamp() {
		return placedTimestamp;
	}

}
