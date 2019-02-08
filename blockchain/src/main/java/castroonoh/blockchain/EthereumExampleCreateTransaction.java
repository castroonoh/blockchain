package castroonoh.blockchain;

import java.io.IOException;
import java.math.BigInteger;

import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

/***
 * This class shown how to simply create a transaction in ethereum
 * @author castro
 */
public class EthereumExampleCreateTransaction {
	
	public static void main(String[] args) {
		Web3j web3j = Web3j.build(new HttpService("https://ropsten.infura.io/{YOUR_TOKEN}") ); //  in url put the right token
		String privateKey = args[0];
		Credentials credentials = Credentials.create(privateKey);
		
		// get the next available nonce
		EthGetTransactionCount ethGetTransactionCount = null;
		try {
			ethGetTransactionCount = web3j.ethGetTransactionCount(
					credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
		} catch (IOException e) {
			e.printStackTrace();
		}
		BigInteger nonce = ethGetTransactionCount.getTransactionCount();
		
		// create transaction
		RawTransactionManager rawManager = new RawTransactionManager(web3j, credentials);
		RawTransaction raw = RawTransaction.createTransaction(
				nonce, // NONCE
				DefaultGasProvider.GAS_PRICE, // GAS PRICE
				BigInteger.valueOf(100000), // GAS LIMIT
				"0x0000000000000000000000000000000000000000", // TO 
				Convert.fromWei("1", Convert.Unit.ETHER).toBigInteger(), // AMOUNT
				null); // DATA - here you can put data to show on etherscan (hex)
		EthSendTransaction trans = null;
		try {
			trans = rawManager.signAndSend(raw);
		} catch (IOException e) {
			e.printStackTrace();
		}
					
		if(trans==null || trans.hasError()) {
			System.out.println("ERROR");
		} else {
			System.out.println(trans.getTransactionHash());
		}
	}

}
