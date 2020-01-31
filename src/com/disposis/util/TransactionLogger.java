package com.disposis.util;

import com.disposis.StockTrader.Managers.OrderDetails;

public class TransactionLogger extends Logger {
	
	protected static TransactionLogger transLoggerInstance;

	protected TransactionLogger() throws Exception {
		super();
	}

	public TransactionLogger(String filePath) throws Exception {
		super(filePath);
	}
	
	public void logPurchase( OrderDetails od )
	{
		switch ( od.getContract().secType() )
		{
			case OPT:
				logOptionPurchase( od );
				
			default:
				break;
		}
	}
	
	public void logSale( OrderDetails od )
	{
		switch ( od.getContract().secType() )
		{
			case OPT:
				logOptionSale( od );
				
			default:
				break;
		}
	}
	
	protected void logOptionPurchase( OrderDetails od )
	{
		String msg = "TransactionLogger::logOptionPurchase() - Bought option for symbol %s:";
		msg += "\n\tcontractId: %s";
		msg += "\n\texpirationDate: %s";
		msg += "\n\ttype: %s";
		msg += "\n\tstrike: %s";
		msg += "\nPurchase Info:";
		msg += "\n\tStrategy: %s";
		msg += "\n\tbuyDate: %s";
		msg += "\n\tbuyPrice: %f";
		msg += "\n\tqty: %d";
		msg += "\n\tcost: %f";
		msg += "\nOpen Tick Data:";
		msg += "\n\tlastTick: %f";
		msg += "\n\tprevClose: %f";
		
//		message(msg, 
//				optionPosition.getOption().getSymbol(), 
//				optionPosition.getOption().getContractId(), 
//				optionPosition.getOption().getExpirationDate(), 
//				optionPosition.getOption().getType(), 
//				optionPosition.getOption().getStrike()
//		);
	}
	
	protected void logOptionSale( OrderDetails od )
	{
//		if ( optionPosition.isOpen() )
//			throw new IllegalArgumentException("TransactionLogger::logOptionPurchase() - optionPosition cannot be open to log a purchase.");
		
		String msg = "TransactionLogger::logOptionPurchase() - Bought option for symbol %s:";
		msg += "\n\tcontractId: %s";
		msg += "\n\texpirationDate: %s";
		msg += "\n\ttype: %s";
		msg += "\n\tstrike: %s";
		msg += "\nPurchase Info:";
		msg += "\n\tBuy Strategy: %s";
		msg += "\n\tbuyDate: %s";
		msg += "\n\tbuyPrice: %f";
		msg += "\n\tqty: %d";
		msg += "\n\tcost: %f";
		msg += "\nOpen Tick Data:";
		msg += "\n\tlastTick: %f";
		msg += "\n\tprevClose: %f";
		msg += "\nSale Info:";
		msg += "\n\tSell Strategy: %s";
		msg += "\n\tsellDate: %s";
		msg += "\n\tsellPrice: %f";
		msg += "\n\tRevenue: %f";
		msg += "\n\tProfit: %f";
		
//		message(msg, 
//				optionPosition.getOption().getSymbol(), 
//				optionPosition.getOption().getContractId(), 
//				optionPosition.getOption().getExpirationDate(), 
//				optionPosition.getOption().getType(), 
//				optionPosition.getOption().getStrike()
//		);
	}

	public static TransactionLogger getInstance()
	{
		try {
			Configuration conf = Configuration.getInstance();
			
			return TransactionLogger.getInstance(conf.getTransLogFilePath() );
		} catch (Exception e) {
			System.out.println(String.format( "Exception caught while trying to create TransactionLogger instance. Msg: %s", e.toString() ));
			e.printStackTrace();
			return null;
		}
	}
	
	public static TransactionLogger getInstance( String filePath )
	{
		if ( Logger.specInsts.get(filePath) == null )
		{
			try {
				Logger.specInsts.put( filePath, new TransactionLogger( filePath ) );
			} catch (Exception e) {
				System.out.println(String.format( "Exception caught while trying to create TransactionLogger instance. Msg: %s", e.toString() ));
				e.printStackTrace();
				return null;
			}
		}
			
		return (TransactionLogger) Logger.specInsts.get(filePath);
	}
}
