package com.disposis.StockTrader.Managers;

import com.disposis.StockTrader.APIConnector.ExecutionDetailsMessage;
import com.disposis.StockTrader.APIConnector.IBMessage;
import com.disposis.StockTrader.APIConnector.OpenOrderEndMessage;
import com.disposis.StockTrader.APIConnector.OpenOrderMessage;
import com.disposis.StockTrader.APIConnector.OrderStatusMessage;
import com.disposis.StockTrader.DataRetrievers.AsynchronousDataRetriever;
import com.disposis.util.Configuration;
import com.disposis.util.Logger;
import com.disposis.util.TransactionLogger;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderManager extends AsynchronousDataRetriever {
	
	protected class OrderRequest extends DataRequest
	{
		protected final int orderId;
		protected final OrderDetails orderDetails;
		protected OrderState orderState;
		
		public OrderRequest( int requestId, BlockingQueue<DataRequest> completedRequestsQueue, OrderDetails od )
		{
			super( requestId, completedRequestsQueue );
			
			this.orderId = od.getOrderId();
			this.orderDetails = od;
		}
		
		public int getOrderId()
		{
			return orderId;
		}
		
		public OrderDetails getOrderDetails() {
			return orderDetails;
		}
		
		public void setOrderState( OrderState os )
		{
			orderState = os;
		}
		
		public OrderState getOrderState()
		{
			return orderState;
		}
		
	};
	
	private static OrderManager instance;
	
	protected Configuration conf = Configuration.getInstance();
	protected Logger logger = Logger.getInstance();
	protected TransactionLogger transLogger = TransactionLogger.getInstance();
	
	protected BlockingQueue<DataRequest> completedRequestsQueue = new LinkedBlockingQueue<DataRequest>();
	
	protected Map<Integer, OrderDetails> ordersById = new HashMap<Integer, OrderDetails>();
	protected Map<OrderStatus, Map<Integer, OrderDetails>> ordersByStatus = new HashMap<OrderStatus, Map<Integer, OrderDetails>>();

	private OrderManager() {
		
		super();
		
		this.waitTimeoutMs = 100;
		
		ordersByStatus.put(OrderStatus.OPEN, new HashMap<Integer, OrderDetails>());
		ordersByStatus.put(OrderStatus.FILLED, new HashMap<Integer, OrderDetails>());
		ordersByStatus.put(OrderStatus.CANCELLED, new HashMap<Integer, OrderDetails>());
	}
	
	public void placeOrder( Contract contract, Order order )
	{
		int orderId = connector.getAndIncrementCurrentOrderId();
		order.orderId(orderId);
		
		OrderDetails od = new OrderDetails(orderId, contract, order, LocalDateTime.now(ZoneId.of("America/New_York")));
		
		ordersById.put(orderId, od);
		ordersByStatus.get(OrderStatus.OPEN).put(orderId, od);

		super.addNewRequest( new OrderRequest( orderId, completedRequestsQueue, od ) );
		
		transLogger.logPurchase(od);
	}
	
	@Override
	protected void handleCompletedRequest(DataRequest completedRequest) {
		
		super.handleCompletedRequest(completedRequest);
		
		OrderRequest or = (OrderRequest) completedRequest;
		OrderDetails od = or.getOrderDetails();
		
		od.setCompletedTimestamp( LocalDateTime.now( ZoneId.of("America/New_York") ) );
		
		transLogger.logSale(od);
	}

	protected void processMessage( IBMessage msg )
	{
		if ( msg instanceof OpenOrderMessage )
		{
			OpenOrderMessage dataMessage = (OpenOrderMessage) msg;
			
			logger.trace("OrderManager::processDataMessage() - OpenOrderMessage for order id %d received; setting status to OPEN_CONFIRMED.", dataMessage.getOrderId());
			
			OrderRequest or = (OrderRequest) inProgressRequests.get(dataMessage.getOrderId());
			
			if (or == null)
				return;
			
			OrderState orderState = dataMessage.getOrderState();
			OrderDetails orderDetails = ordersById.get( or.getOrderId() );
			
			switch ( orderState.getStatus() )
			{
				case "filled":
					logger.trace("OrderManager::processMessage() - Order status for order id %d is filled; updating accordingly.", dataMessage.getOrderId());
					
					ordersByStatus.get( OrderStatus.OPEN ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.FILLED ).put( orderDetails.getOrderId(), orderDetails);
					
					orderDetails.setOrderStatus( OrderStatus.FILLED );
				break;
				
				case "apicanceled":
				case "cancelled":
				case "inactive":
					logger.trace("OrderManager::processMessage() - Order status for order id %d is cancelled/apicancelled/inactive; updating accordingly.", dataMessage.getOrderId());
					
					ordersByStatus.get( OrderStatus.OPEN ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.FILLED ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.CANCELLED ).put( orderDetails.getOrderId(), orderDetails);
					
					orderDetails.setOrderStatus( OrderStatus.CANCELLED );
				break;
				
				default:
					orderDetails.setOrderStatus( OrderStatus.OPEN_CONFIRMED );
			}
			
			if ( orderDetails.getOrderStatus() == OrderStatus.FILLED || orderDetails.getOrderStatus() == OrderStatus.CANCELLED )
			{
				handleCompletedRequest(or);
			}
			
		}
		else if ( msg instanceof OpenOrderEndMessage )
		{
			OpenOrderEndMessage dataMessage = (OpenOrderEndMessage) msg;
			
			logger.trace("OrderPlacer::processMessage() - OpenOrderEndMessage for order id %d received; I have no idea what the purpose of this is so we're just gonna ignore it.", dataMessage.getOrderId());
		}
		else if ( msg instanceof ExecutionDetailsMessage )
		{
			ExecutionDetailsMessage dataMessage = (ExecutionDetailsMessage) msg;
			
			OrderRequest or = (OrderRequest) inProgressRequests.get(dataMessage.getOrderId());
			
			if ( or == null )
				return;
			
			Execution executionDetails = dataMessage.getExecutionDetails();
			OrderDetails orderDetails = ordersById.get( or.getOrderId() );

			int cumulativeQty = executionDetails.cumQty();
			
			orderDetails.setAvgPrice(executionDetails.avgPrice() );
			
			if ( cumulativeQty > 0 && cumulativeQty < orderDetails.getOrder().totalQuantity() )
			{
				orderDetails.setOrderStatus(OrderStatus.PARTIALLY_FILLED);
			}
			else if ( cumulativeQty == orderDetails.getOrder().totalQuantity() )
			{
				orderDetails.setOrderStatus(OrderStatus.FILLED);
				
				ordersByStatus.get( OrderStatus.OPEN ).remove(orderDetails.getOrderId());
				ordersByStatus.get( OrderStatus.FILLED ).put( orderDetails.getOrderId(), orderDetails);
				
				handleCompletedRequest(or);
			}
		}
		else if ( msg instanceof OrderStatusMessage )
		{
			OrderStatusMessage dataMessage = (OrderStatusMessage) msg;
			
			logger.trace("OrderPlacer::processMessage() - OrderStatusMessage for order id %d received.", dataMessage.getOrderId());
			
			OrderRequest or = (OrderRequest) inProgressRequests.get(dataMessage.getOrderId());
			
			if (or == null)
				return;
			
			OrderDetails orderDetails = ordersById.get( or.getOrderId() );
			
			switch ( dataMessage.getStatus().toLowerCase() )
			{
				case "filled":
					logger.trace("OrderPlacer::processMessage() - Order status for order id %d is filled; updating accordingly.", dataMessage.getOrderId());
					
					ordersByStatus.get( OrderStatus.OPEN ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.FILLED ).put( orderDetails.getOrderId(), orderDetails);
					
					orderDetails.setOrderStatus( OrderStatus.FILLED );
				break;
				
				case "apicanceled":
				case "cancelled":
				case "inactive":
					logger.trace("OrderPlacer::processMessage() - Order status for order id %d is cancelled/apicancelled/inactive; updating accordingly.", dataMessage.getOrderId());
					
					ordersByStatus.get( OrderStatus.OPEN ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.FILLED ).remove(orderDetails.getOrderId());
					ordersByStatus.get( OrderStatus.CANCELLED ).put( orderDetails.getOrderId(), orderDetails);
					
					orderDetails.setOrderStatus( OrderStatus.CANCELLED );
				break;
			}
			
			if ( (orderDetails.getOrderStatus() == OrderStatus.OPEN || orderDetails.getOrderStatus() == OrderStatus.OPEN_CONFIRMED) && dataMessage.getFilled() != 0 )
			{
				logger.trace("OrderPlacer::processMessage() - Order id %d has been partially filled; updating accordingly.", dataMessage.getOrderId());
				
				orderDetails.setFilledQty(dataMessage.getFilled());
				orderDetails.setOrderStatus( OrderStatus.PARTIALLY_FILLED );
			}
			
			if ( orderDetails.getOrderStatus() == OrderStatus.FILLED )
			{
				handleCompletedRequest(or);
			}
		}
		else
		{
			logger.error("OrderPlacer::processMessage() - Received unknown IBMessage type for ticker id " + msg.getTickerId() );
		}
	}
	
	protected void sendRequest(int tickerId, DataRequest dataRequest ) {
		
		if ( dataRequest instanceof OrderRequest )
		{
			OrderRequest or = (OrderRequest) dataRequest;
			
			connector.placeOrder(or.getOrderId(), or.getOrderDetails().getContract(), or.getOrderDetails().getOrder(), messageQueues);
		}
		else
		{
			throw new IllegalArgumentException("OrderPlacer::sendRequest() - DataRequest must be an OrderRequest.");
		}
	}
	
	protected void requestExecutions( int tickerId ) {
		connector.getExecutions(tickerId, null, messageQueues );
	}
	
	protected void onWake()
	{
		synchronized ( messageQueues )
		{
			if ( inProgressRequests.size() > 0 )
				requestExecutions( getAndIncrementTickerId() );
		}
	}
	
	public static OrderManager getInstance()
	{
		if ( instance == null )
			instance = new OrderManager();
		
		return instance;
	}
}
