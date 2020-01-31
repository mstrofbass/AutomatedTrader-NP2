package com.disposis.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Logger {
	
	public static final int LOG_LEVEL_TRACE = 10;
	public static final int LOG_LEVEL_DEBUG = 20;
	public static final int LOG_LEVEL_MESSAGE = 30;
	public static final int LOG_LEVEL_ERROR = 40;
	public static final int LOG_LEVEL_FATAL_ERROR = 45;
	
	protected static HashMap<String, Logger> specInsts = new HashMap<String, Logger>();
	protected static Logger instance = null;
	
	protected Configuration conf;
	protected PrintWriter logFileWriter;
	

	protected Logger() throws Exception {
		this.conf = Configuration.getInstance();
		this.logFileWriter = new PrintWriter( new FileWriter( new File( this.conf.getLogFilePath() ) ), true );
	}
	
	protected Logger( String filePath ) throws Exception {
		this.conf = Configuration.getInstance();
		this.logFileWriter = new PrintWriter( new FileWriter( filePath ), true );
	}
	
	protected void writeMessage( String level, String out )
	{	
		this.logFileWriter.println( level + " " + ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/Chicago")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + out );
	}
	
	public void trace( String out )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_TRACE )
			return;

		writeMessage( "[TRACE]", out );
	}
	
	public void trace( String out, Object...args )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_TRACE )
			return;
		
		trace( String.format(out, args));
	}
	
	public void debug( String out )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_DEBUG )
			return;
		
		writeMessage( "[DEBUG]", out );
	}
	
	public void debug( String out, Object...args )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_DEBUG )
			return;
		
		debug( String.format(out, args));
	}
	
	public void error( String out )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_ERROR )
			return;
		
		writeMessage( "[ERROR]", out );
		//System.out.println( "[ERROR] " + out );
	}
	
	public void error( String out, Object...args )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_ERROR )
			return;
		
		error( String.format(out, args));
	}
	
	public void fatalError( String out )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_FATAL_ERROR )
			return;
		
		writeMessage( "[ERROR]", out );
		System.out.println( "[ERROR] " + out );
	}
	
	public void fatalError( String out, Object...args )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_FATAL_ERROR )
			return;
		
		fatalError( String.format(out, args));
	}
	
	public void message( String out )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_MESSAGE )
			return;
		
		writeMessage( "[MESSAGE]", out );
	}
	
	public void message( String out, Object...args )
	{
		if ( this.conf.getLogLevel() > Logger.LOG_LEVEL_MESSAGE )
			return;
		
		message( String.format(out, args));
	}
	
	public void console( String out )
	{
		message( out );
		System.out.println( out );
	}
	
	public void console( String out, Object...args )
	{
		String formattedString = String.format(out, args);
		
		message( formattedString );
		System.out.println( formattedString );
	}
	
	public void console( String out, boolean writeToLog )
	{
		if ( writeToLog )
			console( out );
		
		System.out.println( out );
	}
	
	public void console( String out, boolean writeToLog, Object...args )
	{
		String formattedString = String.format(out, args);
		
		console(formattedString, writeToLog);
	}
	
	public static Logger getInstance()
	{
		if ( instance == null )
		{
			try {
				instance = new Logger();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
			
		return instance;
	}
	
	public static Logger getInstance( String filePath )
	{
		if ( Logger.specInsts.get(filePath) == null )
		{
			try {
				Logger.specInsts.put( filePath, new Logger( filePath ) );
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
			
		return Logger.specInsts.get(filePath);
	}
}
