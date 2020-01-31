package com.disposis.StockTrader.APIConnector;

public class IBMessageWrapper {
	protected IBMessage message;
	
	public IBMessageWrapper( IBMessage message )
	{
		this.message = message;
	}

	public IBMessage getMessage() {
		return message;
	}

	public void setMessage(IBMessage message) {
		this.message = message;
	}
}