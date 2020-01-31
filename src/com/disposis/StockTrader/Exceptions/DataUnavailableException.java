package com.disposis.StockTrader.Exceptions;

public class DataUnavailableException extends Exception {

	private static final long serialVersionUID = 1L;
	
	protected boolean temporary = false;
	protected boolean requestInProgress = false;

	public DataUnavailableException( boolean temporary, boolean requestInProgress ) {
		this.temporary = temporary;
		this.requestInProgress = requestInProgress;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public boolean isRequestInProgress() {
		return requestInProgress;
	}
}
