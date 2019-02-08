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
 * This example shown how to update a wallet in bitcoin. It is useful if you have a wallet that never use in an application, and the first time you
 * will call it you won't find any transaction in it. It could take very long time to download all the blockchain, so think about to use it maybe it will be
 * more useful to create a new wallet and send it the balance
 * @author castro
 */
public class BitcoinExampleUpdateWalletFromZero {
	
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
		// here we update the wallet
		w = updateWallet(w, walletAppKit, key);
		// the balance after
		System.out.println(w.getBalance().value);
	}
	
	/***
	 * this method update the wallet
	 * @param wallet
	 * @param walletAppKit
	 * @param key
	 * @return the updated wallet
	 */
	private static Wallet updateWallet(Wallet wallet, WalletAppKit walletAppKit, ECKey key) {
		Wallet w2 = null;
		try {
			w2 = Wallet.loadFromFile(new File("my.wallet"), null);
		} catch (UnreadableWalletException e) {
			walletAppKit.stopAsync();
			throw new RuntimeException(e);
		}
		w2.importKey(key);
		wallet = w2;
		// see the actual height
		BlockChain chain = walletAppKit.chain();
		// set the height of the last block seen of the wallet to zero
		wallet.setLastBlockSeenHeight(0);
		// add the wallet to the chain, this will update the height with the one of the wallet
		chain.addWallet(wallet);
		// see the new height (it will be 0)
		long h2 = chain.getChainHead().getHeight();
		// add the wallet to the peer group to set the height of this to the heigth of the wallet
		walletAppKit.peerGroup().addWallet(wallet);
		// download the blocks to close the differences in heights from 0 to the actual
		// (this will take time to update the wallet with all txs that you didn't see before)
		walletAppKit.peerGroup().downloadBlockChain();
		try {
			// save the wallet updated
			wallet.saveToFile(new File("my.wallet"));
		} catch (IOException e) {
			walletAppKit.stopAsync();
			throw new RuntimeException(e);
		}
		return wallet;
	}

}
