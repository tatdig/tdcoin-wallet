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

import de.schildbach.tdcwallet.WalletApplication;
import de.schildbach.tdcwallet.data.BlockchainStateLiveData;
import de.schildbach.tdcwallet.data.SelectedExchangeRateLiveData;
import de.schildbach.tdcwallet.data.WalletBalanceLiveData;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

/**
 * @author Andreas Schildbach
 */
public class WalletBalanceViewModel extends AndroidViewModel {
    private final WalletApplication application;
    private BlockchainStateLiveData blockchainState;
    private WalletBalanceLiveData balance;
    private SelectedExchangeRateLiveData exchangeRate;

    public WalletBalanceViewModel(final Application application) {
        super(application);
        this.application = (WalletApplication) application;
    }

    public BlockchainStateLiveData getBlockchainState() {
        if (blockchainState == null)
            blockchainState = new BlockchainStateLiveData(application);
        return blockchainState;
    }

    public WalletBalanceLiveData getBalance() {
        if (balance == null)
            balance = new WalletBalanceLiveData(application);
        return balance;
    }

    public SelectedExchangeRateLiveData getExchangeRate() {
        if (exchangeRate == null)
            exchangeRate = new SelectedExchangeRateLiveData(application);
        return exchangeRate;
    }
}
