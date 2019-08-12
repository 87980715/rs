package BinanceJavaAPIPackage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.TickerStatistics;
import com.pack.binance.utils.ComputeValue;

/**
 * @author Rishabh
 * This class is for test number 2.
 * This Class is responsible for all the trades related executions and will execute the data if conditions satisfied.
 */


public class InitiaterClass {

	private NavigableMap<BigDecimal, BigDecimal> buyBids;
	private NavigableMap<BigDecimal, BigDecimal> sellBids;
	boolean buyingDone = false;
	public BigDecimal buyAvgPrice;
	public BigDecimal sellAvgPrice;
	public BigDecimal stopLoss;
	public BigDecimal orderSellPrice ;
	private String symbol;
	
	TradeExchange trdexg = new TradeExchange();

	/**
	 * In Constructor we will pass the Bids and sell Map which will be coming from
	 * the local DB
	 * 
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 */
	public InitiaterClass(String symbol) throws ClassNotFoundException, InterruptedException {

		this.symbol = symbol;

		fetchTradeData();
	}

	private Boolean calculateLossProfit() {
		
		if(orderSellPrice.compareTo(this.buyAvgPrice)>0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * 
	 * This function is soleley responsible to fetch the data which was saved by the local cached memory and will loop through all the Unexecuted trade
	 */
	
	private void fetchTradeData() throws ClassNotFoundException, InterruptedException {

		try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/binance", "root", "root")) {
			try (PreparedStatement st = con.prepareStatement(
					"select distinct(order_id) from binance.liveorderbook where trading_done<>true and symbol = '"
							+ this.symbol + "'")) {

				ResultSet rs = st.executeQuery();
				while (rs.next()) {

					int orderId = rs.getInt("order_id");
					buyBids = new TreeMap<BigDecimal, BigDecimal>();
					sellBids = new TreeMap<BigDecimal, BigDecimal>();

					setTradingData(orderId, con);
					calculateTrades();
					if(buyingDone) {
					if(calculateLossProfit()) {
						System.out.print("profit");
					}else {
						System.out.print("loss");
					}
										
					}else {
						continue;
					}
				}

			}
		} catch (SQLException e) {
			
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param orderId
	 * @param con
	 * This function is responsible for the setting up the buys and sells list and will set it to the local class variables
	 *
	 */
	
	private void setTradingData(int orderId, Connection con) {

		try (PreparedStatement st = con
				.prepareStatement("select quantity,price,status from binance.liveorderbook where order_id = " + orderId
						+ " and symbol = '" + this.symbol + "'and trading_done = " + false)) {

			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				
				if (rs.getString("status").equals("S")) {

					this.sellBids.put(rs.getBigDecimal("price"),rs.getBigDecimal("quantity"));

				} else if (rs.getString("status").equals("B")) {
					this.buyBids.put(rs.getBigDecimal("price"),rs.getBigDecimal("quantity"));
				}

			}
		}

		catch (Exception e) {
		}

	}

	/**
	 * The is the place where All the conditions will be checked to make the trade.
	 */

	public void calculateTrades() throws ClassNotFoundException, InterruptedException {

		if (getUsd() && getAltCoin() && trdexg.calculateBids(buyBids) && trdexg.calculatesells(sellBids)) {
			Boolean buyStatus = false;
			// if we passed the condition the open the 10 sec window.
			for(int i =0 ;i<10;i++) {
				//long start = System.currentTimeMillis();
				Thread.sleep(1000);
				if(trdexg.calculateBids(buyBids) && trdexg.calculatesells(sellBids)) {
					buyStatus = true;
					continue;
				}else {
					buyStatus = false;
					break;
				}
			}
			this.buyingDone = buyStatus;

			if (this.buyingDone) {
				this.buyAvgPrice = 	calculateBuyAveragePrice();
				this.stopLoss = ComputeValue.percentage(buyAvgPrice, new BigDecimal(0.93)); // stop loss is setting to 93 
				this.sellAvgPrice = ComputeValue.percentage(buyAvgPrice, new BigDecimal(1.05));
				int count = 0;
				while(true) {
					BigDecimal currentBuyAvgPrice = calculateBuyAveragePrice();
					if(currentBuyAvgPrice.compareTo(this.stopLoss) <= 0 ) {
						orderSellPrice = currentBuyAvgPrice ;
					}
					if(currentBuyAvgPrice.compareTo(this.sellAvgPrice) > -1) {
						orderSellPrice = currentBuyAvgPrice;
						break;
					}else {
						Thread.sleep(1000);
						if(count == 20) {
							this.sellAvgPrice = ComputeValue.percentage(buyAvgPrice, new BigDecimal(1.01));
						}
						count++;
						continue;				
					}

				}

			}
		}
	}

	/**
	 * This function is responsible to get the delta in BTCUSDT last 24 hours change in percent;
	 * 
	 */
	
	private boolean getUsd() {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();

		BinanceApiRestClient client = factory.newRestClient();

		TickerStatistics tickerStatistics = client.get24HrPriceStatistics("BTCUSDT");

		if (Float.parseFloat(tickerStatistics.getPriceChangePercent()) < 3
				&& Float.parseFloat(tickerStatistics.getPriceChangePercent()) > -3.0)

			return true;
		return false;

	}

	/**
	 * This function is responsible for the fetching up the delta for selected altcoin last 15 minutes 
	 * 
	 */
	
	private boolean getAltCoin() {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
	    BinanceApiRestClient client = factory.newRestClient();
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(),CandlestickInterval.FIFTEEN_MINUTES);
		System.out.println(candlestickBars.size());
	
		
		System.out.println(candlestickBars.get(0).getClose()+" , "+candlestickBars.get(candlestickBars.size()-1).getClose());
		BigDecimal latestPrice = new BigDecimal(candlestickBars.get(0).getClose());
		BigDecimal lastPrice = new BigDecimal(candlestickBars.get(candlestickBars.size()-1).getClose());
		
		BigDecimal result = latestPrice.subtract(lastPrice);
		result = result.divide(latestPrice.add(lastPrice),3,RoundingMode.HALF_UP);
		result = result.multiply(new BigDecimal(100));
		System.out.println("Result is "+result);
		if(Double.parseDouble(result.toString())<1.5 && Double.parseDouble(result.toString()) > -1.5) 
			return true;
		return false;
	}

	private BigDecimal calculateBuyAveragePrice() {

		BigDecimal maxBuyPrice = this.buyBids.firstKey();
		BigDecimal lowSellPrice = this.sellBids.lastKey();

		BigDecimal avgPrice = maxBuyPrice.add(lowSellPrice);
		avgPrice = avgPrice.divide(new BigDecimal(2.0));
		avgPrice = ComputeValue.percentage(avgPrice, new BigDecimal(1.005));

		return avgPrice;
	}

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * 
	 * This is the initiator class for this Where you have to give the Altcoin you want to trade with
	 */
	
	public static void main(String args[]) throws ClassNotFoundException, InterruptedException {

		Scanner scannerObj = new Scanner(System.in);
		System.out.println("Enter the combinations seperated with space you want to trade with -");
		String combinations = scannerObj.nextLine();
		String[] words = combinations.split("\\ ");

		for (int i = 0; i < words.length; i++) {

			new InitiaterClass(words[i]);

		}

		scannerObj.close();

	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

}
