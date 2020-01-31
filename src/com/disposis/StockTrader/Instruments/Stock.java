package com.disposis.StockTrader.Instruments;

import java.util.HashSet;
import java.util.Set;

import org.boon.json.annotations.JsonIgnore;

import com.ib.client.Contract;
import com.ib.client.Types;


public class Stock extends TradeableInstrument {

	// Contract is generated on demand, so this is never actually used; solely here so the json parser will ignore it
	@JsonIgnore
	protected Contract contract;

	protected String longName;
	protected String category;
	protected String subcategory;
	
	protected Set<String> industries = new HashSet<String>();
	
	public Stock() {}
	
	public Stock( int contractId, String symbol ) {
		super(contractId, symbol);
		
		this.securityType = Types.SecType.STK;
	}

	public String getLongName() {
		return longName;
	}

	public void setLongName(String longName) {
		this.longName = longName;
	}

	public boolean hasIndustry( String industry )
	{
		return industries.contains(industry);
	}
	
	public Set<String> getIndustries() {
		return industries;
	}

	public void setIndustries(Set<String> industries) {
		
		this.industries = new HashSet<String>();
		
		for ( String industry : industries )
			addIndustry( industry );
	}

	public void addIndustry( String industry )
	{
		this.industries.add(industry);
	}
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public void setSubcategory(String subcategory) {
		this.subcategory = subcategory;
	}
	
	@JsonIgnore
	public Contract getContract()
	{
		return new StockContract(contractId, symbol, primaryExchange);
	}

	@Override
	public String toString() {
		return "Stock [symbol=" + symbol + ", primaryExchange=" + primaryExchange + ", contractId=" + contractId + "]";
	}
}
