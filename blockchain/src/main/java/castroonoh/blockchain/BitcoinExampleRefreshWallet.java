package castroonoh.blockchain;

import java.io.File;
import java.io.IOException;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

/***
 * This class shown how to refresh a wallet saved in a file in a bitcoin context.
 * It is useful when you don't use it for a long time and when you restart your app doesn't find any coins/satoshis on the wallet that the walletAppKit give you
 * Another useful case is when you have a pending transaction on your wallet and your application crash. When you will restart it, maybe your tx is already mined 
 * but you will see it in pending yet (this was my case).
 * @author castro
 */
public class BitcoinExampleRefreshWallet {

	public static void main(String[] args) {
		byte[] priv = Hex.decode(args[0]);
		ECKey key = ECKey.fromPrivate(priv);

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
				e.printStackTrace();
			}
		} else {
			w.importKey(key);
		}
		// the balance before
		System.out.println(w.getBalance().value);
		// here we check the wallet
		w = checkOrReset(w, walletAppKit, key);
		// the balance after
		System.out.println(w.getBalance().value);
	}
	
	/***
	 * this method check the wallet get by walletAppKit and compare it with the one saved in the file
	 * if they are not equal it used the file version to update it
	 * it is useful because when you stop the application, the wallet saved in the file stop to update itself
	 * and when you restart the app, the wallet in the walletAppKit has got balance equal zero
	 * @param wallet
	 * @param walletAppKit
	 * @param key
	 * @return the updated wallet
	 */
	private static Wallet checkOrReset(Wallet wallet, WalletAppKit walletAppKit, ECKey key) {
		// if our wallet is zero, check if it is updated
		if(wallet.getBalance().isZero()) {
			Wallet w2 = null;
			try {
				w2 = Wallet.loadFromFile(new File("my.wallet"), null);
			} catch (UnreadableWalletException e) {
				walletAppKit.stopAsync();
				throw new RuntimeException(e);
			}
			w2.importKey(key);
			// I take the pending txs of both wallets
			long txPen1 = wallet.getPendingTransactions().size();
			long txPen2 = w2.getPendingTransactions().size();
			// if the number of transactions in the wallets are different or these are equals but these are in different status
			if(!(w2.getTransactions(true).size() != wallet.getTransactions(true).size()) || txPen1 != txPen2) {
				// update the wallet with the one saved in the file
				wallet = w2;
				// if there are pending transactions
				if(txPen2!=0) {
					if(wallet.getPendingTransactions().size()>0) {
						// see the actual height
						BlockChain chain = walletAppKit.chain();
						long h1 = chain.getChainHead().getHeight();
						// add the wallet to the chain, this will update the height with the one of the wallet
						chain.addWallet(wallet);
						// see the new height
						long h2 = chain.getChainHead().getHeight();
						// if the wallet height is less than the actual
						if(h2<h1) {
							// add the wallet to the peer group to set the height of this to the heigth of the wallet
							walletAppKit.peerGroup().addWallet(wallet);
							// download the blocks to close the differences in heights (this will update the wallet with all txs that you didn't see before)
							walletAppKit.peerGroup().downloadBlockChain();
							try {
								// save the wallet updated
								wallet.saveToFile(new File("my.wallet"));
							} catch (IOException e) {
								walletAppKit.stopAsync();
								throw new RuntimeException(e);
							}
						}
					}
				}
			}
		}
		return wallet;
	}

}
