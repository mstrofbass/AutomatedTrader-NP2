package com.disposis.StockTrader.DataRetrievers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.disposis.StockTrader.MarketInfo;
import com.disposis.StockTrader.APIConnector.ContractDetailsMessage;
import com.disposis.StockTrader.APIConnector.IBMessage;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;

public abstract class ContractDetailsDataRetriever extends AsynchronousDataRetriever {
	
	protected class ContractDetailsRequest extends DataRequest
	{
		public final Contract contract;
		public List<ContractDetails> contractDetails = new ArrayList<ContractDetails>();
		
		public ContractDetailsRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue, Contract contract )
		{
			super( requestId, completedRequestsQueue );
			this.contract = contract;
		}
	};
	
	protected MarketInfo marketInfo;
	
	protected ContractDetailsDataRetriever() 
	{ 
		super();
		
		this.marketInfo = MarketInfo.getInstance();
	}
	
	protected void processMessage( IBMessage msg )
	{
		if ( msg instanceof ContractDetailsMessage )
		{
			ContractDetailsMessage dataMessage = (ContractDetailsMessage) msg;
			
			logger.trace("ContractDetailsDataRetriever::processMessage() - Contract details for ticker id %d received.", dataMessage.getTickerId());
			
			ContractDetailsRequest cdd = (ContractDetailsRequest) inProgressRequests.get(msg.getTickerId());
			
			if (cdd == null)
				return;
			
			cdd.contractDetails.add( dataMessage.getContractDetails() );
			
		}
		else
		{
			logger.error("ContractDetailsDataRetriever::processDataMessage() - Received unknown IBMessage type for ticker id " + msg.getTickerId() );
		}
	}

	@Override
	protected void handleEndOfStream(DataRequest dataRequest) {
		logger.trace( "ContractDetailsDataRetriever::handleEndOfStream() - Received end of stream message for ticker id %d", dataRequest.getTickerId() );
		
		super.handleEndOfStream(dataRequest);
		
		handleCompletedRequest(dataRequest);
	}

	@Override
	protected void sendRequest(int tickerId, DataRequest dataRequest ) {
		
		if ( dataRequest instanceof ContractDetailsRequest )
		{
			ContractDetailsRequest cdr = (ContractDetailsRequest) dataRequest;
			
			connector.getContractDetailsData(tickerId, messageQueues, cdr.contract );
		}
		else
		{
			throw new IllegalArgumentException("ContractDetailsDataRetriever::sendRequest() - DataRequest must be a contract details request.");
		}
	}
 }