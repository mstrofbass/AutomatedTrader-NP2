package com.disposis.StockTrader.DataRetrievers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.disposis.StockTrader.APIConnector.IBMessage;
import com.disposis.StockTrader.DataRetrievers.AsynchronousDataRetriever.DataRequest;

public class MessageQueues {

	protected final BlockingQueue<DataRequest> newRequestsQueue = new LinkedBlockingQueue<DataRequest>();
	protected final BlockingQueue<IBMessage> responseMessagesQueue = new LinkedBlockingQueue<IBMessage>();
	
	public MessageQueues() {
		super();
	}

	public BlockingQueue<DataRequest> getNewRequestsQueue() {
		return newRequestsQueue;
	}

	public BlockingQueue<IBMessage> getResponseMessagesQueue() {
		return responseMessagesQueue;
	}
}
