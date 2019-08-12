package BinanceJavaAPIPackage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;

public class CurrencyExecuter {

	BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("API-KEY", "SECRET");
	BinanceApiRestClient client = factory.newRestClient();

	public void getCurrencyList() {
		OrderBook orderBook = client.getOrderBook("NEOETH", 10);
		List<OrderBookEntry> asks = orderBook.getAsks();
		OrderBookEntry firstAskEntry = asks.get(0);
		System.out.println(firstAskEntry.getPrice() + " / " + firstAskEntry.getQty());

	}

	public void printDBVALUE() throws ClassNotFoundException, InterruptedException {

		  
		try {
			Connection con=DriverManager.getConnection(  
			"jdbc:mysql://localhost:3306/Binance","root","root");
			Statement stmt=con.createStatement(); 
			ResultSet rs=stmt.executeQuery("select * from LiveOrderBook");  
			
			while(rs.next()) {
				System.out.println(rs.getInt("OrderId"));
				Thread.sleep(1000);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
}
