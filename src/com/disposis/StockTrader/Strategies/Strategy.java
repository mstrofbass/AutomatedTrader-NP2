package com.disposis.StockTrader.Strategies;

import com.disposis.StockTrader.Instruments.Stock;
import com.disposis.StockTrader.Managers.OptionManager;
import com.disposis.StockTrader.Managers.TickDataManager;
import com.disposis.StockTrader.MarketInfo;
import com.disposis.util.Configuration;
import com.disposis.util.Logger;
import com.disposis.util.TransactionLogger;
import java.time.LocalDateTime;

public abstract class Strategy {
		
	public class BuyAction 
	{
		protected final String label;
		protected final String version;
		protected LocalDateTime buyDateTime;
		
		public BuyAction( String label, String version, LocalDateTime buyDateTime )
		{
			this.label = label;
			this.version = version;
			this.buyDateTime = buyDateTime;
		}

		public LocalDateTime getBuyDateTime() {
			return buyDateTime;
		}

		public void setBuyDateTime(LocalDateTime buyDateTime) {
			this.buyDateTime = buyDateTime;
		}

		public String getLabel() {
			return label;
		}

		public String getVersion() {
			return version;
		}
	}
	
	protected static int defaultMultiplier = 100;
	
	protected Configuration conf;
	protected Logger logger;
	protected TransactionLogger transLogger;
	protected MarketInfo marketInfo;
	
	protected TickDataManager tickDataManager;
	protected OptionManager optionManager;
	
	protected final String label;
	protected final String version;

	protected Strategy( String label, String version ) {
		this.conf = Configuration.getInstance();
		this.logger = Logger.getInstance();
		this.transLogger = TransactionLogger.getInstance();
		
		this.tickDataManager = TickDataManager.getInstance();
		this.optionManager = OptionManager.getInstance();
		
		marketInfo = MarketInfo.getInstance();
		
		this.label = label;
		this.version = version;
	}
	
	abstract public void analyze( Stock stock, boolean toOpen, boolean toClose ) throws Exception;
	
	public String getLabel()
	{
		return label;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	protected static double getPercentChange( double x, double y )
	{
		return (( y - x ) / x ) * 100;
	}
}
