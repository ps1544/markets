package com.jps.finserv.markets.equities;

public class DataDailyMarket implements Comparable<DataDailyMarket>{

	private String name;
	private String symbol;
	private double open;
	private double high;
	private double low;
	private double close;
	private double  netChange;
	private double pcntChange;
	private int volume;
	private double high52Week;
	private double low52Week;
	private double dividend;
	private double  yield;
	private double peRatio;
	private double ytdChange;
	private String exchange;
	
	
	public DataDailyMarket(String name,  String symbol, double open,  double high,
												double low, double close, double netChange,  double pcntChange,
												int volume,  double high52Week, double low52Week, double dividend,
												double yield, double peRatio, double ytdChange, String exchange)
	{
		super();
		this.name = name;
		this.symbol = symbol;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.netChange= netChange;
		this.pcntChange = pcntChange;
		this.volume = volume;
		this.high52Week = high52Week;
		this.low52Week = low52Week;
		this.dividend = dividend;
		this.yield = yield;
		this.peRatio = peRatio;
		this.ytdChange  = ytdChange;
		this.exchange = exchange;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
	public double  getOpen() {
		return open;
	}
	public void setOpen(double open) {
		this.open= open;
	}
		
	public double  getHigh() {
		return high;
	}
	public void setHigh(double high) {
		this.high = high;
	}
	
	public double  getLow() {
		return low;
	}
	public void setLow(double low) {
		this.low= low;
	}
	
	public double  getClose() {
		return close;
	}
	
	public void setClose(double close) {
		this.close = close;
	}
		
	public double  getNetChange() {
		return netChange;
	}
	public void setNetChange(double netChange) {
		this.netChange= netChange;
	}
	
	public double  getPcntChange() {
		return pcntChange;
	}
	public void setPcntChange(double pcntChange) {
		this.pcntChange = pcntChange;
	}
	
	public int  getVolume() {
		return volume;
	}
	public void setVolume(int volume) {
		this.volume = volume;
	}
		
	public double  getHigh52Week() {
		return high52Week;
	}
	public void setHigh52Week(double  high52Week) {
		this.high52Week = high52Week;
	}
	
	public double  getLow52Week() {
		return low52Week;
	}
	public void setgetLow52Week(double  low52Week) {
		this.low52Week = low52Week;
	}
	
	public double  getDividend() {
		return dividend;
	}
	public void setDividend(double dividend) {
		this.dividend = dividend;
	}
	
	public double  getYield() {
		return yield;
	}
	public void setYield(double yield) {
		this.yield = yield;
	}
	
	public double  getPeRatio() {
		return peRatio;
	}
	public void setPeRatio(double  peRatio) {
		this.peRatio = peRatio;
	}
	
	public double  getYtdChange() {
		return ytdChange;
	}
	public void setYtdChange(double  ytdChange) {
		this.ytdChange = ytdChange;
	}
	
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
		
	public int compareTo(DataDailyMarket temp) {

		double compareVolume = ((DataDailyMarket) temp).getPcntChange();

		//ascending order
		//return this.volume- compareVolume;

		//descending order
		//return compareVolume  - this.netChange;
		 return new Double(compareVolume).compareTo( this.pcntChange);
	}
}