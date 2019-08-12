package BinanceJavaAPIPackage;

import java.math.BigDecimal;
import java.util.NavigableMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.pack.binance.model.LiveOrderBook;
import com.pack.binance.repo.LiveOrderBookRepository;

public class TestDb {
	  
	@Autowired
	LiveOrderBookRepository liveOrderBookRepository;
	
	  
	 private void saveData(Integer count) {
		  //NavigableMap<BigDecimal, BigDecimal> askData =  getAsks();
		  LiveOrderBook orderBook = new LiveOrderBook(); 
	      
		  //for(BigDecimal key :askData.keySet()) {
			  orderBook.setOrderId(count);
			  orderBook.setSymbol("ETCBNC");
			  orderBook.setPrice(12.34);
			  orderBook.setQuantity(34.23);
			  liveOrderBookRepository.save(orderBook);		  
	     // }
	  }

     public static void main(String srgs[]) {
    	 TestDb ob = new TestDb();
    	 
    	 ob.saveData(1);
     }

}
