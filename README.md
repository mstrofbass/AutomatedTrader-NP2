# AutomatedTrader-NP

NOTE: Something in my process periodically does weird things with tabs so ignore the random tabbing in the source code. 

This repository contains code related to a two part stock trading application. The other part ingests large amounts of stock-related data and simulates stock/option trades based on proprietary strategies. The data is then fed into Google BigQuery and analyzed in various ways. (Original analysis was performed on a small Hadoop cluster set up in my house until I realized BigQuery does everything I need and I don't have to spend 90% of my time managing a the node cluster.)

Once an "optimal" strategy is identified, the strategy is translated into actionable code and put into the automated trading portion (this repository) of the application. For confidentiality reasons, the actual trading strategy has been removed.

This particular code integrates into an automated trading API from Interactive Brokers. The brokerage provides a library that handles communication with an application running locally (thankfully...communications between the library and the trading application are really low-level). The library effectively provides hooks to perform various actions that are (almost) all centered around a pub/sub mechanism. In particular, you call a particular method to subscribe to a certain set of data and a different method is called when the library received related data. So, for example, one might subscribe to "stock tick data" (stock price data that is updated on each change) and the library will repeatedly call the same method each time new a stock price changes.

What this ends up necessitating, in short, is the development of a multithreaded application that is capable of reacting to and consuming asynchronous data. I'm particularly proud of this project because I did not have any significant multithreaded application experience prior to starting this project.

That being said, it's still a bit of a mess given the learn-as-you-go approach and no time to do a complete refactor (which is needed long term). But it works. 

An easy things to note initially: the `util` folder contains some basic utilities developed for the application, including a basic, home grown logging utility. It was easier to just write something basic than work with one of the popular libraries. (Ignore the weird tabbing...either NetBeans of Git seems to like to screw them up on me.)

The general pattern employed for this application is that during booting of the application, various "managers" are initialized. Each is manager typically associated with a particular type of data. For example, there's a manager for stock data (metadata about a particular stock), tick data (stock pricing data), and one for initiating/tracking trades. Once initialized, the application subscribes to tick data for the stocks in question. The tick data manager keeps track of the current stock price. Periodically, the main controller initiates a strategy analysis which accesses the current stock prices for the target stocks, analyzes the prices for certain trigger conditions, and, if a trigger condition is met, triggers an order using the order manager. The order manager submits and tracks the order, making dynamic changes to the order depending on the specific order type. When the order is fulfilled, it records additional trigger conditions for a corresponding sell order. Similarly, the strategy analysis determines whether a position should be closed and, if necessary, creates the appropriate sell order.

High-level package overview: 

* `src.com.disposis.StockTrader.APIConnector` - These provide an object-oriented (OO) abstraction to the API provided by the broker. In particular, it translates method calls into correspond Message objects that are handled by the appropriate manager.
* `src.com.disposis.StockTrader.Instruments` - OO abstraction of the various types of financial instruments involved, such as stocks and options. A little weird because of the difficulty in completely decoupling it from the underlying API.
* `src.com.disposis.StockTrader.Managers` - The meat of the application, so to speak. Most of these include at least one BlockingQueue (asynchronous, thread-safe queue). When a call to the API is made, the manager passes the corresponding queue to the API wrapper. When a corresponding response is received, the response is translated into the appropriate message from the APIConnector class and inserted into the queue. The manager waits for a new message to be added to the queue (hence BlockingQueue) and processes each message added to the queue.
* `src.com.disposis.StockTrader.Strategies` - Contains the proprietary trading strategies in code form. The proprietary aspects have been removed, so only the higher level functionality is visible.
* `src.com.disposis.StockTrader.StockTrader.java` - Contains the main() method, acting as the entry point and kicks off the Controller.
* `src.com.disposis.StockTrader.Controller.java` - Initializes everything and contains the loop that triggers the analysis.
* `src.com.disposis.StockTrader.MarketInfo.java` - Simple helper class for determining things like whether the market is open.

Although still in what would probably be considered a late alpha/early beta phase, it functions well enough for extended testing.
