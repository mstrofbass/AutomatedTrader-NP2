package com.disposis.StockTrader.Instruments;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.ib.client.Contract;
import com.ib.client.Types;
import com.ib.client.Types.SecType;

public class OptionContract extends Contract
{
	public OptionContract(String underlyingSymbol, LocalDate expirationDate, Types.Right type ) 
    {
        symbol(underlyingSymbol);
        secType(SecType.OPT.name());
        exchange("SMART");
        currency("USD");
        
        lastTradeDateOrContractMonth( expirationDate.format( DateTimeFormatter.BASIC_ISO_DATE ) );
        right( type );
        multiplier("100");
    }
	
	public OptionContract(String underlyingSymbol, LocalDate expirationDate, Types.Right type, double strike ) 
    {
        symbol(underlyingSymbol);
        secType(SecType.OPT.name());
        exchange("SMART");
        currency("USD");
        
        lastTradeDateOrContractMonth( expirationDate.format( DateTimeFormatter.BASIC_ISO_DATE ) );
        right( type );
        strike(strike);
        multiplier("100");
    }
	
    public OptionContract(int contractId, String underlyingSymbol, LocalDate expirationDate, Types.Right type, double strike ) 
    {
    		conid(contractId);
        symbol(underlyingSymbol);
        secType(SecType.OPT.name());
        exchange("SMART");
        currency("USD");
        
        lastTradeDateOrContractMonth( expirationDate.format( DateTimeFormatter.BASIC_ISO_DATE ) );
        right( type );
        strike(strike);
        multiplier("100");
    }
}