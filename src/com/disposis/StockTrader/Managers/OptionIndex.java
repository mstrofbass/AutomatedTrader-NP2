package com.disposis.StockTrader.Managers;

import com.disposis.StockTrader.Exceptions.DataNotLoadedException;
import com.disposis.StockTrader.Instruments.Option;
import com.disposis.StockTrader.Instruments.Stock;
import com.disposis.util.Logger;
import com.ib.client.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class OptionIndex {

	protected static OptionIndex instance;
	
	protected Logger logger;
	protected StockManager stockManager;
	
	// underlyingConId, OptionType, Multipler, ExpirationDate, Strike
	protected Map<Integer, Map<Types.Right, Map<Integer, Map<LocalDate, Map< Double, Option>>>>> fullIndex = new HashMap<>();
	protected Map<Integer, List<Option>> optionsByUnderlyingContractId = new HashMap<>();
	protected Map<Integer, Option> optionsByContractId = new HashMap<>();
	
	protected OptionIndex() {
		logger = Logger.getInstance();
		stockManager = StockManager.getInstance();
	}
	
	public Option getByContractId( int contractId ) throws DataNotLoadedException
	{
		if ( !optionsByContractId.containsKey( contractId ) )
			throw new DataNotLoadedException();
		
		return optionsByContractId.get(contractId);
	}
	
	public List<Option> getForStock( Stock stock )  throws DataNotLoadedException
	{
		if ( !optionsByUnderlyingContractId.containsKey( stock.getContractId() ) )
			throw new DataNotLoadedException();
		
		return optionsByUnderlyingContractId.get( stock.getContractId() );
	}
	
	public Set<LocalDate> getExpirationDates( int underlyingContractId, Types.Right optionType, int multiplier ) throws DataNotLoadedException
	{
		if ( !fullIndex.containsKey(underlyingContractId))
		{
			throw new DataNotLoadedException( String.format("Data for underlying contract id %s is not loaded.", underlyingContractId ) );
		}
		
		Map<Types.Right, Map<Integer, Map<LocalDate, Map<Double, Option>>>> rightsIndex = fullIndex.get(underlyingContractId);
		
		if ( rightsIndex == null )
		{
			return null;
		}
		
		Map<Integer, Map<LocalDate, Map<Double, Option>>> multiplierIndex = rightsIndex.get(optionType);
		
		if (multiplierIndex == null )
		{
			return null;
		}
		
		Map<LocalDate, Map<Double, Option>> expirationDateIndex = multiplierIndex.get(multiplier);
		
		if ( expirationDateIndex == null )
		{
			return null;
		}
		
		return expirationDateIndex.keySet();
	}
	
	public Set<Double> getStrikes( int underlyingContractId, Types.Right optionType, int multiplier, LocalDate expirationDate ) throws DataNotLoadedException
	{
		if ( !fullIndex.containsKey(underlyingContractId))
		{
			throw new DataNotLoadedException( String.format("Data for underlying contract id %s is not loaded.", underlyingContractId ) );
		}
		
		Map<Types.Right, Map<Integer, Map<LocalDate, Map<Double, Option>>>> rightsIndex = fullIndex.get(underlyingContractId);
		
		if ( rightsIndex == null )
		{
			return null;
		}
		
		Map<Integer, Map<LocalDate, Map<Double, Option>>> multiplierIndex = rightsIndex.get(optionType);
		
		if (multiplierIndex == null )
		{
			return null;
		}
		
		Map<LocalDate, Map<Double, Option>> expirationDateIndex = multiplierIndex.get(multiplier);
		
		if ( expirationDateIndex == null )
		{
			return null;
		}
		
		Map<Double, Option> strikeIndex = expirationDateIndex.get(expirationDate);
		
		if ( strikeIndex == null )
		{
			return null;
		}
		
		return strikeIndex.keySet();
	}
	
	public Option getOption( int underlyingContractId, Types.Right optionType, int multiplier, LocalDate expirationDate, double strike ) throws DataNotLoadedException
	{
		if ( !fullIndex.containsKey(underlyingContractId))
		{
			throw new DataNotLoadedException( String.format("Data for underlying contract id %s is not loaded.", underlyingContractId ) );
		}
		
		Map<Types.Right, Map<Integer, Map<LocalDate, Map<Double, Option>>>> rightsIndex = fullIndex.get(underlyingContractId);
		
		if ( rightsIndex == null )
		{
			return null;
		}
		
		Map<Integer, Map<LocalDate, Map<Double, Option>>> multiplierIndex = rightsIndex.get(optionType);
		
		if (multiplierIndex == null )
		{
			return null;
		}
		
		Map<LocalDate, Map<Double, Option>> expirationDateIndex = multiplierIndex.get(multiplier);
		
		if ( expirationDateIndex == null )
		{
			return null;
		}
		
		Map<Double, Option> strikeIndex = expirationDateIndex.get(expirationDate);
		
		if ( strikeIndex == null )
		{
			return null;
		}
		
		return strikeIndex.get(strike);
	}
	
	public void addToIndex(Option option)
	{
		optionsByContractId.put(option.getContractId(), option);
		
		if ( !optionsByUnderlyingContractId.containsKey( option.getUnderlyingContractId() ) )
		{
			optionsByUnderlyingContractId.put(option.getUnderlyingContractId(), new ArrayList<>());
		}
		
		optionsByUnderlyingContractId.get(option.getUnderlyingContractId()).add(option);
		
		addToFullIndex( option );
	}
	
	protected void addToFullIndex( Option option )
	{
		Map<Types.Right, Map<Integer, Map<LocalDate, Map< Double, Option>>>> rightsIndex = fullIndex.get(option.getUnderlyingContractId());
		
		if ( rightsIndex == null )
		{
			rightsIndex = new HashMap<>();
			fullIndex.put(option.getUnderlyingContractId(), rightsIndex );
		}
		
		Map<Integer, Map<LocalDate, Map< Double, Option>>> multiplierIndex = rightsIndex.get(option.getType());
		
		if ( multiplierIndex == null )
		{
			multiplierIndex = new HashMap<>();
			rightsIndex.put(option.getType(), multiplierIndex );
		}
		
		Map<LocalDate, Map< Double, Option>> expirationDateIndex = multiplierIndex.get(option.getMultiplier());
		
		if ( expirationDateIndex == null )
		{
			expirationDateIndex = new HashMap<>();
			multiplierIndex.put(option.getMultiplier(), expirationDateIndex );
		}
		
		Map<Double, Option> strikeIndex = expirationDateIndex.get(option.getExpirationDate());
		
		if ( strikeIndex == null )
		{
			strikeIndex = new HashMap<>();
			expirationDateIndex.put(option.getExpirationDate(), strikeIndex );
		}
		
		strikeIndex.put(option.getStrike(), option);
	}
	
	public static OptionIndex getInstance()
	{
		if ( instance == null )
			instance = new OptionIndex();
		
		return instance;
	}
}
