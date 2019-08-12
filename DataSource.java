package BinanceJavaAPIPackage;

import java.sql.Connection;
import java.sql.SQLException; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder; 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment; 
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
 

@Configuration
@PropertySource("classpath:jdbc.properties")
@AutoConfigureOrder(1)
public class DataSource {
		
	private static HikariDataSource ds;
	 
	@Autowired Environment ENV;	


	private static HikariConfig config = new HikariConfig();
  
	    @Bean
	    @Primary
	    public HikariDataSource getDataSouce() {    
		    try {  
				  _createDataSource();
			        
				 // m_oEnv = env;
			} catch (Exception e) {
				e.printStackTrace();
			} 
		    
		    return ds;
	    }

	    
	    private void _createDataSource() {
			
	    	if(ds == null) {
		    	String database = ENV.getProperty("jdbc.database");
		    	String serverIp = ENV.getProperty("server.ip");
		    	String portNo = ENV.getProperty("jdbc.port");
		    	String userName = ENV.getProperty("jdbc.username");
		    	String password = ENV.getProperty("jdbc.password");
		    	
		    	StringBuilder urlBuilder = new StringBuilder();
				urlBuilder.append("jdbc:mysql://").append(serverIp).append(":").append(portNo).append("/").append(database).append("?").append("user=")
					.append(userName).append("&password=").append(password).append("&verifyServerCertificate=false&useSSL=true&autoReconnect=true&useAffectedRows=true");
		    	
		        config.setJdbcUrl( urlBuilder.toString() );
		        config.setUsername(  ENV.getProperty("jdbc.username") );
		        config.setPassword( ENV.getProperty("jdbc.password") );
		        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		        config.addDataSourceProperty( "cachePrepStmts" , "true" );
		        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		        config.addDataSourceProperty( "useServerPrepStmts" , "true" );
		        config.addDataSourceProperty( "maxStatements" , "500" );
		        config.addDataSourceProperty( "testConnectionOnCheckin" , "false" );
		        config.addDataSourceProperty( "testConnectionOnCheckout" , "true" );
		        config.setMaxLifetime(2000000);
		        config.setMaximumPoolSize(20);
		        config.setAutoCommit(true);
		        config.setMinimumIdle(20);
		        config.setConnectionTimeout(20000);
		        ds = new HikariDataSource( config );
	    	}
			
		}

		public static Connection getConnection() throws SQLException {
	        return ds.getConnection();
	    }
		
		public static Connection getConnectionTransaction() throws SQLException {
			Connection objCon = ds.getConnection();
			objCon.setAutoCommit(false);
	        return objCon;
	    }


}
