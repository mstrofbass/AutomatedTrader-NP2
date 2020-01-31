package com.disposis.StockTrader.APIConnector;

import com.ib.client.Contract;
import com.ib.client.Execution;

public final class ExecutionDetailsMessage extends IBMessage {

	protected final Contract contract;
	protected final Execution executionDetails;
	
	public ExecutionDetailsMessage( int orderId, Contract contract, Execution executionDetails )
	{
		super(orderId);
		
		this.contract = contract;
		this.executionDetails = executionDetails;
	}
	
	public int getOrderId()
	{
		return getTickerId();
	}

	public Contract getContract() {
		return contract;
	}

	public Execution getExecutionDetails() {
		return executionDetails;
	}
}