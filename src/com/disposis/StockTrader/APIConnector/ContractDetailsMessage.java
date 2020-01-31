package com.disposis.StockTrader.APIConnector;

import com.ib.client.ContractDetails;

public final class ContractDetailsMessage extends IBMessage {

	private final ContractDetails contractDetails;
	
	
	public ContractDetailsMessage( int tickerId, ContractDetails contractDetails )
	{
		super(tickerId);
		
		this.contractDetails = contractDetails;
	}

	public ContractDetails getContractDetails()
	{
		return this.contractDetails;
	}
	
}
