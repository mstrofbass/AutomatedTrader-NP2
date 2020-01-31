package com.disposis.StockTrader.DataRetrievers;

import com.disposis.StockTrader.APIConnector.IBConnector;
import com.disposis.StockTrader.APIConnector.IBErrorMessage;
import com.disposis.StockTrader.APIConnector.IBMessage;
import com.disposis.StockTrader.APIConnector.StreamEndMessage;
import com.disposis.util.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AsynchronousDataRetriever implements Runnable {
	
	public abstract class DataRequest 
	{
		public final int requestId;
		private final BlockingQueue<DataRequest> completedRequestsQueue;
		
		public boolean error = false;
		public IBErrorMessage errorMessage;
		
		private int tickerId;
		
		protected DataRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue )
		{
			this.requestId = requestId;
			this.completedRequestsQueue = completedRequestsQueue;
		}

		public int getTickerId() {
			return tickerId;
		}

		protected void setTickerId(int tickerId) {
			this.tickerId = tickerId;
		}

		protected BlockingQueue<DataRequest> getCompletedRequestsQueue() {
			return completedRequestsQueue;
		}
		
		public boolean hasError()
		{
			return error;
		}
	}

	private static final AtomicInteger tickerId = new AtomicInteger( 1 );
	
	protected volatile boolean run = true;
	
	protected final Logger logger;
	protected final IBConnector connector;
	protected Map<Integer, DataRequest> inProgressRequests = new HashMap<>();
	
	protected long waitTimeoutMs = 0;
	
	protected final MessageQueues messageQueues;
	
	protected AsynchronousDataRetriever() 
	{ 
		this.logger = Logger.getInstance();
		this.connector = IBConnector.getInstance();
		this.messageQueues = new MessageQueues(); 
	}
	
	@Override
	public void run() 
	{
		logger.trace("DataRetriever::run() - Entering run method. ");
		logger.trace("DataRetriever::run() - Entering run loop.");
		
		int currentTickerId;
		int currentRequestId;
		DataRequest newRequest;
		IBMessage newResponseMessage;
		
		synchronized ( messageQueues )
		{
			while( run ) 
			{
				while ( ( newRequest = messageQueues.getNewRequestsQueue().poll() ) != null )
				{
					currentTickerId = tickerId.getAndIncrement();
					currentRequestId = newRequest.requestId;
					
					logger.trace("DataRetriever::run() - Sending off request for data using request id %s and ticker id %s.", currentRequestId, currentTickerId);
					
					newRequest.setTickerId(currentTickerId);
					inProgressRequests.put(currentTickerId, newRequest );
					
					sendRequest(currentTickerId, newRequest);
				}
				
				while ( ( newResponseMessage = messageQueues.getResponseMessagesQueue().poll() ) != null )
				{
					consume(newResponseMessage);
				}
				
				try {
					messageQueues.wait( waitTimeoutMs );
				}
				catch ( InterruptedException e )
				{
					break;
				}
				
				onWake();
			}
		}
	}

	protected void consume(IBMessage x) 
	{ 
		logger.trace("DataRetriever::consume() - IBMessage for ticker id %s received on message queue; entering consume method.", x.getTickerId());
		
		DataRequest dataRequest = inProgressRequests.get(x.getTickerId());
		
		if ( x instanceof IBErrorMessage )
		{
			IBErrorMessage error = (IBErrorMessage) x;
			
			if ( dataRequest != null && error.getCode() != IBErrorMessage.MARKET_DATA_NOT_SUBSCRIBED )
			{
				handleErroredRequest( dataRequest, error );
			}
			else if ( dataRequest == null )
			{
				logger.error("DataRetriever::consume() - Received IBErrorMessage for unknown ticker id " + x.getTickerId() );
				logger.error("DataRetriever::consume() - Error message recevied from the connector. Error code: %d Error Message %s", error.getCode(), error.getMessage() );
			}
			else
			{
				logger.error("DataRetriever::consume() - Error message recevied from the connector. Error code: %d Error Message %s", error.getCode(), error.getMessage() );
			}
			
			return;
		}
		
		if ( x instanceof StreamEndMessage )
		{
			logger.trace("DataRetriever::consume() - Received StreamEndMessage for ticker id %s on message queue.", x.getTickerId());
			
			if ( dataRequest != null )
				handleEndOfStream( dataRequest );
			else
			{
				logger.trace("DataRetriever::consume() - Received StreamEndMessage but there is no data request for ticker id %s.", x.getTickerId());
			}
		}
		else if ( x instanceof IBMessage )
		{
			IBMessage dataMessage = (IBMessage) x;
			processMessage( dataMessage );
		}
		else
		{
			logger.error("DataRetriever::consume() - Received unknown IBMessage type for ticker id " + x.getTickerId() );
		}
	}
	
	protected void handleCompletedRequest( DataRequest completedRequest )
	{
		logger.trace("DataRetriever::handleCompletedRequest() - Completing request for ticker id " + completedRequest.getTickerId() );
		
		connector.removeMessageQueue( completedRequest.getTickerId() );
		
		if ( inProgressRequests.containsKey( completedRequest.getTickerId() ) )
		{
			try {
				completedRequest.getCompletedRequestsQueue().put( inProgressRequests.remove( completedRequest.getTickerId() ) );
			} catch (InterruptedException e) {
				logger.error("IBConnector::error() - received an interruptedException and have no idea what to do with it.");
			}
		}
	}
	
	protected void handleErroredRequest( DataRequest erroredRequest, IBErrorMessage error )
	{
		erroredRequest.error = true;
		erroredRequest.errorMessage = error;
		
		handleCompletedRequest(erroredRequest);
	}
	
	protected abstract void sendRequest( int tickerId, DataRequest dataRequest );
	
	protected abstract void processMessage( IBMessage msg );
	
	protected void onWake()
	{
		return;
	}
	
	protected void handleEndOfStream( DataRequest dataRequest )
	{
		return;
	}
	
	protected void addNewRequest( DataRequest newRequest )
	{
		logger.debug("DataRetriever::addNewRequest() - Adding new request for request id " + newRequest.requestId );
		
		synchronized ( messageQueues )
		{
			try {
				messageQueues.getNewRequestsQueue().put( newRequest );
				messageQueues.notifyAll();
				
			} catch (InterruptedException e) {
				logger.error("DataRetriever::addNewRequest() - Received an interrupted exception and I have no idea what to do with it." );
				e.printStackTrace();
			}
		}
	}
	
	protected MessageQueues getMessageQueues()
	{
		return messageQueues;
	}

	protected int getAndIncrementTickerId()
	{
		return tickerId.getAndIncrement();
	}
	
	public void shutdown()
	{
		run = false;
	}
}
