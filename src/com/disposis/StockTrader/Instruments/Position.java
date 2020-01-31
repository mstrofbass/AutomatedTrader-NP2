package com.disposis.StockTrader.Instruments;

import com.disposis.StockTrader.Strategies.Strategy.BuyAction;

public abstract class Position {
	
	protected final TradeableInstrument instrument;
	protected final BuyAction buyAction;
	protected BuyAction sellAction;
	
	public Position(TradeableInstrument instrument, BuyAction buyAction) {
		
		this.instrument = instrument;
		this.buyAction = buyAction;
	}

	public TradeableInstrument getInstrument() {
		return instrument;
	}

	public BuyAction getBuyAction() {
		return buyAction;
	}
	
	public boolean isOpen()
	{
		return sellAction == null;
	}
}
