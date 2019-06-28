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

package de.schildbach.tdcwallet.ui;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.tdcoinj.core.Address;
import org.tdcoinj.core.ECKey;
import org.tdcoinj.core.LegacyAddress;
import org.tdcoinj.crypto.DeterministicKey;
import org.tdcoinj.wallet.Wallet;

import com.google.common.collect.Iterables;

import de.schildbach.tdcwallet.Constants;
import de.schildbach.tdcwallet.WalletApplication;
import de.schildbach.tdcwallet.data.AbstractWalletLiveData;
import de.schildbach.tdcwallet.data.AddressBookEntry;
import de.schildbach.tdcwallet.data.WalletLiveData;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * @author Andreas Schildbach
 */
public class SendingAddressesViewModel extends AndroidViewModel {
    private final WalletApplication application;
    public final WalletLiveData wallet;
    public LiveData<List<AddressBookEntry>> addressBook;
    public final AddressesToExcludeLiveData addressesToExclude;
    public final ClipLiveData clip;
    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Address>> showEditAddressBookEntryDialog = new MutableLiveData<>();

    public SendingAddressesViewModel(final Application application) {
        super(application);
        this.application = (WalletApplication) application;
        this.wallet = new WalletLiveData(this.application);
        this.addressesToExclude = new AddressesToExcludeLiveData(this.application);
        this.clip = new ClipLiveData(this.application);
    }

    public class AddressesToExcludeLiveData extends AbstractWalletLiveData<Set<String>> {
        public AddressesToExcludeLiveData(final WalletApplication application) {
            super(application);
        }

        @Override
        protected void onWalletActive(final Wallet wallet) {
            loadAddressesToExclude();
        }

        private void loadAddressesToExclude() {
            final Wallet wallet = getWallet();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    final List<ECKey> derivedKeys = wallet.getIssuedReceiveKeys();
                    Collections.sort(derivedKeys, DeterministicKey.CHILDNUM_ORDER);
                    final List<ECKey> randomKeys = wallet.getImportedKeys();

                    final Set<String> addresses = new HashSet<>(derivedKeys.size() + randomKeys.size());
                    for (final ECKey key : Iterables.concat(derivedKeys, randomKeys))
                        addresses.add(LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key).toString());
                    postValue(addresses);
                }
            });
        }
    }

    public static class ClipLiveData extends LiveData<ClipData> implements OnPrimaryClipChangedListener {
        private final ClipboardManager clipboardManager;

        public ClipLiveData(final WalletApplication application) {
            clipboardManager = (ClipboardManager) application.getSystemService(Context.CLIPBOARD_SERVICE);
        }

        @Override
        protected void onActive() {
            clipboardManager.addPrimaryClipChangedListener(this);
            onPrimaryClipChanged();
        }

        @Override
        protected void onInactive() {
            clipboardManager.removePrimaryClipChangedListener(this);
        }

        @Override
        public void onPrimaryClipChanged() {
            setValue(clipboardManager.getPrimaryClip());
        }

        public void setClipData(final ClipData clipData) {
            clipboardManager.setPrimaryClip(clipData);
        }
    }
}
