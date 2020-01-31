package com.disposis.StockTrader.Instruments;

import java.time.LocalDate;

import com.ib.client.Contract;
import com.ib.client.Types;

public class Option extends TradeableInstrument {
	
	private String underlyingSymbol;
	private double strike;
	protected int underlyingContractId;
	
    private final Types.Right type;	
    private final LocalDate expirationDate;
    private final int multiplier = 100;
    
    public Option(int contractId, String symbol, LocalDate expirationDate, Types.Right type) {
		
		super(contractId, symbol);
		
		this.type = type;
		this.expirationDate = expirationDate;
		this.securityType = Types.SecType.OPT;
	}
    
    public Option(int contractId, String symbol, LocalDate expirationDate, Types.Right type, double strike) {
		
    		super(contractId, symbol);
    		
		this.type = type;
		this.strike = strike;
		this.expirationDate = expirationDate;
		this.securityType = Types.SecType.OPT;
	}

	public String getUnderlyingSymbol() {
		return underlyingSymbol;
	}
	
	public void setUnderlyingSymbol( String underlyingSymbol ) {
		this.underlyingSymbol = underlyingSymbol;
	}

	public Types.Right getType() {
		return type;
	}

	public double getStrike() {
		return strike;
	}

	public LocalDate getExpirationDate() {
		return expirationDate;
	}
	
	public int getMultiplier()
	{
		return multiplier;
	}
	
	public int getUnderlyingContractId() {
		return underlyingContractId;
	}

	public void setUnderlyingContractId(int underlyingContractId) {
		this.underlyingContractId = underlyingContractId;
	}

	@Override
	public Contract getContract() {
		if ( contractId != 0 )
			return new OptionContract( contractId, underlyingSymbol, expirationDate, type, strike);
		else if (strike != 0 )
			return new OptionContract( symbol, expirationDate, type, strike);
		else
			return new OptionContract( symbol, expirationDate, type);
	}
}
