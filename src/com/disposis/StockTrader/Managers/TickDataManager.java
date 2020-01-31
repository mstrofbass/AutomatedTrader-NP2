package com.disposis.StockTrader.Managers;

import com.disposis.StockTrader.APIConnector.IBConnector;
import com.disposis.StockTrader.APIConnector.IBMessage;
import com.disposis.StockTrader.APIConnector.TickDataMessage;
import com.disposis.StockTrader.DataRetrievers.AsynchronousDataRetriever;
import com.disposis.StockTrader.Instruments.TradeableInstrument;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TickDataManager extends AsynchronousDataRetriever {
	
	protected class TickDataRequest extends DataRequest {
		
		public final TradeableInstrument instrument;
		
		public TickDataRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue, TradeableInstrument instrument ) {
			super( requestId, completedRequestsQueue );
			
			this.instrument = instrument;
		}
	}
	
	protected static TickDataManager instance;
	
	protected Map<Integer, TickData> tickDataByContractId = new HashMap<>();
	protected Map<Integer, Integer> contractIdToTickerIdMap = new HashMap<>();
	
	protected TickDataManager() {
		super();
	}
	
	@Override
	protected void processMessage(IBMessage msg) 
	{
		if ( msg instanceof TickDataMessage )
		{
			TickDataMessage dataMessage = (TickDataMessage) msg;
			
			TickDataRequest tdd = (TickDataRequest) inProgressRequests.get(msg.getTickerId());
			
			if ( tdd == null )
				return;
			
			TickData td = tickDataByContractId.get( tdd.instrument.getContractId() );

			synchronized ( td )
			{
				switch ( dataMessage.getField() )
				{
					case IBConnector.TICK_FIELD_DELAYED_CLOSE:
						logger.debug("StreamingTickDataRetriever::processMessage() - Adding close price for contractId %s ", tdd.instrument.getContractId() );
						td.setPreviousClose( dataMessage.getPrice() );
					break;
					
					case IBConnector.TICK_FIELD_DELAYED_LAST:
						logger.debug("StreamingTickDataRetriever::processMessage() - Adding last tick price for contractId %s ", tdd.instrument.getContractId() );
						td.setLastTick(dataMessage.getPrice());
					break;
				}
			}
		}
		else
		{
			logger.error("StreamingTickDataRetriever::processMessage() - Received unknown IBMessage type for ticker id " + msg.getTickerId() );
		}
	}
	
	synchronized protected void addNewRequest(TickDataRequest newRequest) {
		
		if ( tickDataByContractId.get( newRequest.instrument.getContractId() ) != null )
			return;
		
		super.addNewRequest(newRequest);
		
		tickDataByContractId.put(newRequest.instrument.getContractId(), new TickData());
	}
	
	synchronized public TickData cancelRequest( TradeableInstrument tradeableInstrument )
	{
		int requestTickerId = contractIdToTickerIdMap.get( tradeableInstrument.getContractId() );
		contractIdToTickerIdMap.remove( tradeableInstrument.getContractId() );
		
		cancelRequest( requestTickerId );
		
		return tickDataByContractId.remove(tradeableInstrument.getContractId());
	}
	
	synchronized protected void cancelRequest( int tickerId )
	{
		connector.cancelContractDataRequest(tickerId);
		
		if ( inProgressRequests.get(tickerId) != null )
			handleCompletedRequest( inProgressRequests.get(tickerId) );
	}
	
	public TickData getStreamingTickData( TradeableInstrument instrument )
	{
		if ( !tickDataByContractId.containsKey( instrument.getContractId() ) )
		{
			addNewRequest( new TickDataRequest(0, new LinkedBlockingQueue<>(), instrument ) );
		}
		
		TickData tickData = tickDataByContractId.get( instrument.getContractId() );

		if ( !tickData.isComplete() )
		{
			synchronized( messageQueues )
			{
				while ( !tickData.isComplete() )
				{
					try {
						messageQueues.wait();
					} catch (InterruptedException e) {
						logger.error("StreamingTickDataRetriever::getTickDataLive() - Caught an Interrupted Exception and I have no idea what to do with it.");
						e.printStackTrace();
					}
				}
			}
		}
		
		return tickData;
	}
	
	public TickData getStaticTickData( TradeableInstrument instrument )
	{
		try {
			return (TickData) getStreamingTickData(instrument).clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected void sendRequest(int tickerId, DataRequest dataRequest ) {
		
		if ( dataRequest instanceof TickDataRequest )
		{
			TickDataRequest tdr = (TickDataRequest) dataRequest;
			connector.getContractData(tickerId, tdr.instrument, messageQueues );
			contractIdToTickerIdMap.put(tdr.instrument.getContractId(), tickerId);
		}
		else
		{
			throw new IllegalArgumentException("StreamingTickDataRetriever::sendRequest() - DataRequest must be a TickDataRequest.");
		}
	}

	@Override
	protected void handleCompletedRequest( DataRequest dataRequest ) {
		
		connector.cancelContractDataRequest( dataRequest.getTickerId() );
		
		super.handleCompletedRequest(dataRequest);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		
		for ( int tickerId : contractIdToTickerIdMap.values() )
		{
			cancelRequest(tickerId);
		}
	}

	public static TickDataManager getInstance()
	{
		if ( instance == null )
			instance = new TickDataManager();
		
		return instance;
	}
 }