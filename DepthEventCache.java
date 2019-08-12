package BinanceJavaAPIPackage;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.pack.binance.repo.LiveOrderBookRepository;
/**
 * 
 * @author Rishabh
 * This class is for test number 1.
 */
public class DepthEventCache {

	private static final String BIDS = "BIDS";
	private static final String ASKS = "ASKS";

	int counter = 0;

	private final String symbol;
	private final BinanceApiRestClient restClient;
	private final BinanceApiWebSocketClient wsClient;
	private final WsCallback wsCallback = new WsCallback();
	private final Map<String, NavigableMap<BigDecimal, BigDecimal>> depthCache = new HashMap<>();

	private long lastUpdateId = -1;
	private volatile Closeable webSocket;

	@Autowired
	LiveOrderBookRepository liveOrderBookRepository;

	public DepthEventCache(String symbol) {
		this.symbol = symbol;

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		this.wsClient = factory.newWebSocketClient();
		this.restClient = factory.newRestClient();

		initialize();
	}

	private void initialize() {
		// 1. Subscribe to depth events and cache any events that are received.
		final List<DepthEvent> pendingDeltas = startDepthEventStreaming();

		// 2. Get a snapshot from the rest endpoint and use it to build the initial depth cache.
		initializeDepthCache();

		// 3. & 4. handled in here.
		applyPendingDeltas(pendingDeltas);
	}

	private List<DepthEvent> startDepthEventStreaming() {
		final List<DepthEvent> pendingDeltas = new CopyOnWriteArrayList<>();
		wsCallback.setHandler(pendingDeltas::add);

		this.webSocket = wsClient.onDepthEvent(symbol.toLowerCase(), wsCallback);

		return pendingDeltas;
	}

	private void initializeDepthCache() {
		OrderBook orderBook = restClient.getOrderBook(symbol.toUpperCase(), 10);

		this.lastUpdateId = orderBook.getLastUpdateId();

		NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());
		for (OrderBookEntry ask : orderBook.getAsks()) {
			asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
		}
		depthCache.put(ASKS, asks);

		NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
		for (OrderBookEntry bid : orderBook.getBids()) {
			bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
		}
		depthCache.put(BIDS, bids);
	}

	private void applyPendingDeltas(final List<DepthEvent> pendingDeltas) {
		final Consumer<DepthEvent> updateOrderBook = newEvent -> {
			if (newEvent.getFinalUpdateId() > lastUpdateId) {
				System.out.println(newEvent);
				lastUpdateId = newEvent.getFinalUpdateId();
				updateOrderBook(getAsks(), newEvent.getAsks());
				updateOrderBook(getBids(), newEvent.getBids());
				try {
					printDepthCache();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		final Consumer<DepthEvent> drainPending = newEvent -> {
			pendingDeltas.add(newEvent);

			pendingDeltas.stream().filter(e -> e.getFinalUpdateId() > lastUpdateId) .forEach(updateOrderBook);

			
			wsCallback.setHandler(updateOrderBook);
		};

		wsCallback.setHandler(drainPending);
	}

	private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries,
			List<OrderBookEntry> orderBookDeltas) {
		for (OrderBookEntry orderBookDelta : orderBookDeltas) {
			BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
			BigDecimal qty = new BigDecimal(orderBookDelta.getQty());
			if (qty.compareTo(BigDecimal.ZERO) == 0) {

				lastOrderBookEntries.remove(price);
			} else {
				lastOrderBookEntries.put(price, qty);
			}
		}
	}

	public NavigableMap<BigDecimal, BigDecimal> getAsks() {
		return depthCache.get(ASKS);
	}

	public NavigableMap<BigDecimal, BigDecimal> getBids() {
		return depthCache.get(BIDS);
	}

	private Map.Entry<BigDecimal, BigDecimal> getBestAsk() {
		return getAsks().lastEntry();
	}

	private Map.Entry<BigDecimal, BigDecimal> getBestBid() {
		return getBids().firstEntry();
	}

	public Map<String, NavigableMap<BigDecimal, BigDecimal>> getDepthCache() {
		return depthCache;
	}

	public void close() throws IOException {
		webSocket.close();
	}

	private void saveData(Integer count, String buyOrSell) {

		NavigableMap<BigDecimal, BigDecimal> data = null;
		if (buyOrSell.equalsIgnoreCase("S"))
			data = getAsks();
		else
			data = getBids();
		
		

		for (BigDecimal key : data.keySet()) {
			BigDecimal quantity = key.multiply(data.get(key));
			try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/binance", "root", "root")) {
				try (PreparedStatement st = con.prepareStatement(
						"Insert into  Binance.Liveorderbook(quantity, symbol, price, status, trading_done, order_id) values (?,?,?,?,?,?)")) {

					st.setBigDecimal(1, quantity);
					st.setString(2, this.symbol);
					st.setBigDecimal(3, key);
					st.setString(4, buyOrSell);
					st.setBoolean(5, false);
					st.setInt(6, count);

					int rowsUpdated = st.executeUpdate();
					System.out.println("Rows affected = "+rowsUpdated);

				}
				catch(Exception e ) {
					
					e.printStackTrace();
					
				}
			} catch (Exception e) {
			}

		}
	}

	private void printDepthCache() throws SQLException {

		System.out.println(depthCache);
		counter++;
		saveData(counter, "S");
		saveData(counter, "B");
		

	}

	//This is the place which will start the depthCache streaming event and will store the data to the local database;
	
	public static void main(String[] args) {

		Scanner scannerObj = new Scanner(System.in);
		System.out.println("Enter the combinations you want to save in your local DB -");
		String combination = scannerObj.nextLine();
		if(combination.length()<6) 
			System.out.println("Please enter Valid combination");
		else 
		new DepthEventCache(combination);
		scannerObj.close();
	}

	private final class WsCallback implements BinanceApiCallback<DepthEvent> {

		private final AtomicReference<Consumer<DepthEvent>> handler = new AtomicReference<>();

		@Override
		public void onResponse(DepthEvent depthEvent) {
			try {
				handler.get().accept(depthEvent);
			} catch (final Exception e) {
				System.err.println("Exception caught processing depth event");
				e.printStackTrace(System.err);
			}
		}

		@Override
		public void onFailure(Throwable cause) {
			System.out.println("WS connection failed. Reconnecting. cause:" + cause.getMessage());

			initialize();
		}

		private void setHandler(final Consumer<DepthEvent> handler) {
			this.handler.set(handler);
		}
	}
}
