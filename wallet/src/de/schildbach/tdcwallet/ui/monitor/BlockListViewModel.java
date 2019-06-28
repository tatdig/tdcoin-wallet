/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.tdcwallet.ui.monitor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tdcoinj.core.Sha256Hash;
import org.tdcoinj.core.StoredBlock;
import org.tdcoinj.core.Transaction;
import org.tdcoinj.wallet.Wallet;

import de.schildbach.tdcwallet.Constants;
import de.schildbach.tdcwallet.WalletApplication;
import de.schildbach.tdcwallet.data.AbstractWalletLiveData;
import de.schildbach.tdcwallet.data.AddressBookEntry;
import de.schildbach.tdcwallet.data.AppDatabase;
import de.schildbach.tdcwallet.data.TimeLiveData;
import de.schildbach.tdcwallet.data.WalletLiveData;
import de.schildbach.tdcwallet.service.BlockchainService;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * @author Andreas Schildbach
 */
public class BlockListViewModel extends AndroidViewModel {
    private final WalletApplication application;
    private BlocksLiveData blocks;
    private TransactionsLiveData transactions;
    private WalletLiveData wallet;
    private TimeLiveData time;

    private static final int MAX_BLOCKS = 100;

    public BlockListViewModel(final Application application) {
        super(application);
        this.application = (WalletApplication) application;
        this.addressBook = AppDatabase.getDatabase(this.application).addressBookDao().getAll();
    }

    public BlocksLiveData getBlocks() {
        if (blocks == null)
            blocks = new BlocksLiveData(application);
        return blocks;
    }

    public TransactionsLiveData getTransactions() {
        if (transactions == null)
            transactions = new TransactionsLiveData(application);
        return transactions;
    }

    public WalletLiveData getWallet() {
        if (wallet == null)
            wallet = new WalletLiveData(application);
        return wallet;
    }

    public final LiveData<List<AddressBookEntry>> addressBook;

    public TimeLiveData getTime() {
        if (time == null)
            time = new TimeLiveData(application);
        return time;
    }

    public static class BlocksLiveData extends LiveData<List<StoredBlock>> implements ServiceConnection {
        private final WalletApplication application;
        private final LocalBroadcastManager broadcastManager;
        private BlockchainService blockchainService;

        private BlocksLiveData(final WalletApplication application) {
            this.application = application;
            this.broadcastManager = LocalBroadcastManager.getInstance(application);
        }

        @Override
        protected void onActive() {
            broadcastManager.registerReceiver(broadcastReceiver,
                    new IntentFilter(BlockchainService.ACTION_BLOCKCHAIN_STATE));
            application.bindService(new Intent(application, BlockchainService.class), this, Context.BIND_AUTO_CREATE);
        }

        @Override
        protected void onInactive() {
            application.unbindService(this);
            broadcastManager.unregisterReceiver(broadcastReceiver);
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            blockchainService = ((BlockchainService.LocalBinder) service).getService();
            setValue(blockchainService.getRecentBlocks(MAX_BLOCKS));
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            blockchainService = null;
        }

        private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (blockchainService != null)
                    setValue(blockchainService.getRecentBlocks(MAX_BLOCKS));
            }
        };
    }

    public static class TransactionsLiveData extends AbstractWalletLiveData<Set<Transaction>> {
        private TransactionsLiveData(final WalletApplication application) {
            super(application);
        }

        @Override
        protected void onWalletActive(final Wallet wallet) {
            loadTransactions();
        }

        public void loadTransactions() {
            final Wallet wallet = getWallet();
            if (wallet == null)
                return;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    org.tdcoinj.core.Context.propagate(Constants.CONTEXT);
                    final Set<Transaction> transactions = wallet.getTransactions(false);
                    final Set<Transaction> filteredTransactions = new HashSet<Transaction>(transactions.size());
                    for (final Transaction tx : transactions) {
                        final Map<Sha256Hash, Integer> appearsIn = tx.getAppearsInHashes();
                        if (appearsIn != null && !appearsIn.isEmpty()) // TODO filter by updateTime
                            filteredTransactions.add(tx);
                    }
                    postValue(filteredTransactions);
                }
            });
        }
    }
}
