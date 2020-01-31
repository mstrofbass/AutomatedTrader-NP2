package com.disposis.StockTrader.APIConnector;

public final class IBErrorMessage extends IBMessage {

	public static final int CLIENT_ALREADY_CONNECTED = 501;
	public static final int CLIENT_CONNECTION_ATTEMPT_FAILED = 502;
	public static final int CLIENT_GATEWAY_OUT_OF_DATE = 503;
	public static final int CLIENT_NOT_CONNECTED = 504;
	
	public static final int MARKET_DATA_NOT_SUBSCRIBED = 10167;
	
	private int code;
	private String message;
	
	public IBErrorMessage( int tickerId, int code, String message)
	{
		super(tickerId);
		
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "IBErrorMessage [code=" + code + ", message=" + message + ", toString()=" + super.toString() + "]";
	}
}
