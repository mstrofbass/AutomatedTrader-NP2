package com.disposis.StockTrader.Instruments;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class StockContract extends Contract {
    public StockContract(String symbol) {
        symbol(symbol);
        secType(SecType.STK.name());
        exchange("SMART");
        currency("USD");
    }
    
    public StockContract(String symbol, String primaryExchange) {
        symbol(symbol);
        secType(SecType.STK.name());
        exchange("SMART");
        currency("USD");
        primaryExch(primaryExchange);
    }
    
    public StockContract(int contractId, String symbol, String primaryExchange) {
        conid(contractId);
    		symbol(symbol);
        secType(SecType.STK.name());
        exchange("SMART");
        currency("USD");
        primaryExch(primaryExchange);
    }
}
