package com.disposis.StockTrader;

import com.disposis.util.Configuration;
import com.disposis.util.Logger;
import com.disposis.util.TransactionLogger;

public class StockTrader {
	
	protected static Configuration conf;
	protected static Logger logger;
	protected static TransactionLogger transLogger;

	public static void main(String[] args) throws Exception {
		
		conf = Configuration.getInstance();
		
		if ( conf == null )
		{
			System.out.println("StockTrader::main() - Fatal error: Configuration.getInstance() returned a null Configuration object.");
			System.exit(0);
		}
		
		logger = Logger.getInstance();
		
		if ( logger == null )
		{
			System.out.println("StockTrader::main() - Fatal error: Logger.getInstance() returned a null logger object.");
			System.exit(0);
		}
		
		logger.message("***************************************************");
		logger.trace("StockTrader::main() - Program beginning.");
		logger.trace("StockTrader::main() - Configuration and logger loaded.");
		
		logger.trace("StockTrader::main() - Initializing controller.");
		
		transLogger = TransactionLogger.getInstance();
		
		if ( transLogger == null )
		{
			logger.fatalError("StockTrader::main() - Fatal error: TransactionLogger.getInstance() returned a null logger object.");
			System.exit(0);
		}
		
		Controller controller = new Controller();
		
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            		controller.shutdown();
            }
        });
		
		logger.trace("StockTrader::main() - Calling Controller::process().");
		
		try {
			controller.run();
		}
		catch (Exception e)
		{
			String msg = "StockTrader::main() - Controller::process() threw an exception: " + e.getMessage();
			
			logger.error(msg);
			
			e.printStackTrace();
		}
        
        logger.trace("StockTrader::main() - Controller::process() exited.");
        
        logger.message("StockTrader::main() - Program complete, shutting down.");
        
        System.exit(0);
	}
}
