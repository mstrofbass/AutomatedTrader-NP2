package com.disposis.StockTrader.Instruments;

import com.disposis.StockTrader.Strategies.ReboundStrategy.ReboundStrategyBuyAction;
import com.disposis.StockTrader.Strategies.Strategy.BuyAction;

public class OptionPosition extends Position {
	
	public OptionPosition(Option option, BuyAction buyAction) {
		super( option, buyAction );
	}
	
	public Option getOption() {
		return (Option) instrument;
	}
	
	public ReboundStrategyBuyAction getBuyAction()
	{
		return (ReboundStrategyBuyAction) buyAction;
	}
}
