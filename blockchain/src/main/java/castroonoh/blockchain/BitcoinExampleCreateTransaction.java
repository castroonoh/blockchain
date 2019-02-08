package castroonoh.blockchain;

import java.io.File;
import java.io.IOException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

/***
 * This class shown how to create a simple transaction in Bitcoin
 * This is the most easy way to create it
 * @author castro
 */
public class BitcoinExampleCreateTransaction {

	public static void main(String[] args) {
		// create a key with the private key sent in the args
		byte[] priv = Hex.decode(args[0]);
		ECKey key = ECKey.fromPrivate(priv);

		// we setup the walletAppKit
		WalletAppKit walletAppKit = new WalletAppKit(TestNet3Params.get(), new File("/tmp"), ".spv");
		walletAppKit.setAutoSave(true);
		walletAppKit.setBlockingStartup(true);
		walletAppKit.startAsync();
		walletAppKit.awaitRunning();
		
		// Set up the components and link them together.
		Wallet w = walletAppKit.wallet();
		if(w==null) {
			try {
				w = Wallet.loadFromFile(new File("my.wallet"), null);
			} catch (UnreadableWalletException e) {
				walletAppKit.stopAsync();
				e.printStackTrace();
			}
		} else {
			w.importKey(key);
		}
		
		// we create a transaction object
		Transaction tx = new Transaction(w.getParams());
		// we add the output that we want to send to the address
		tx.addOutput(
				 Coin.valueOf(1000),
				 Address.fromBase58(TestNet3Params.get(), "mgm5ciXburRvnAqTkv2hkg6UBnq7V8UZ36")
				 );
		
		// we prepare the request
		 SendRequest sendRequest = SendRequest.forTx(tx);
		 
		 // Fill-in the missing details for our wallet, eg. fees.
		 try {
			w.completeTx(sendRequest);
		} catch (InsufficientMoneyException e) {
			walletAppKit.stopAsync();
			e.printStackTrace();
		}
		 
		// Broadcast and commit transaction
		 PeerGroup peers = walletAppKit.peerGroup();
		 w.commitTx(tx);
		 peers.broadcastTransaction(tx);
		 
		 // Return a reference to the caller
		 String txHash = tx.getHashAsString();
		 System.out.println(txHash);
		 
		 //save the wallet
		 try {
			w.saveToFile(new File("my.wallet"));
		} catch (IOException e) {
			walletAppKit.stopAsync();
			e.printStackTrace();
		}
		 
		 // stop the sync
		 walletAppKit.stopAsync();
	}

}
