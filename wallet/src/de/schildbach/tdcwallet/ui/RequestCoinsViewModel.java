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

import org.tdcoinj.core.Address;
import org.tdcoinj.core.Coin;
import org.tdcoinj.protocols.payments.PaymentProtocol;
import org.tdcoinj.script.Script;
import org.tdcoinj.uri.TdcoinURI;
import org.tdcoinj.wallet.Wallet;

import de.schildbach.tdcwallet.Constants;
import de.schildbach.tdcwallet.WalletApplication;
import de.schildbach.tdcwallet.data.AbstractWalletLiveData;
import de.schildbach.tdcwallet.data.ConfigOwnNameLiveData;
import de.schildbach.tdcwallet.data.SelectedExchangeRateLiveData;
import de.schildbach.tdcwallet.util.Bluetooth;
import de.schildbach.tdcwallet.util.Qr;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

/**
 * @author Andreas Schildbach
 */
public class RequestCoinsViewModel extends AndroidViewModel {
    private final WalletApplication application;
    public final FreshReceiveAddressLiveData freshReceiveAddress;
    private final ConfigOwnNameLiveData ownName;
    public final SelectedExchangeRateLiveData exchangeRate;
    public final MutableLiveData<Coin> amount = new MutableLiveData<>();
    public final MutableLiveData<String> bluetoothMac = new MutableLiveData<>();
    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
    public final MediatorLiveData<byte[]> paymentRequest = new MediatorLiveData<>();
    public final MediatorLiveData<Uri> tdcoinUri = new MediatorLiveData<>();
    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();

    @Nullable
    public Intent bluetoothServiceIntent = null;

    public RequestCoinsViewModel(final Application application) {
        super(application);
        this.application = (WalletApplication) application;
        this.freshReceiveAddress = new FreshReceiveAddressLiveData(this.application);
        this.ownName = new ConfigOwnNameLiveData(this.application);
        this.exchangeRate = new SelectedExchangeRateLiveData(this.application);
        this.qrCode.addSource(freshReceiveAddress, new Observer<Address>() {
            @Override
            public void onChanged(final Address receiveAddress) {
                maybeGenerateQrCode();
            }
        });
        this.qrCode.addSource(ownName, new Observer<String>() {
            @Override
            public void onChanged(final String label) {
                maybeGenerateQrCode();
            }
        });
        this.qrCode.addSource(amount, new Observer<Coin>() {
            @Override
            public void onChanged(final Coin amount) {
                maybeGenerateQrCode();
            }
        });
        this.qrCode.addSource(bluetoothMac, new Observer<String>() {
            @Override
            public void onChanged(final String bluetoothMac) {
                maybeGenerateQrCode();
            }
        });
        this.paymentRequest.addSource(freshReceiveAddress, new Observer<Address>() {
            @Override
            public void onChanged(final Address receiveAddress) {
                maybeGeneratePaymentRequest();
            }
        });
        this.paymentRequest.addSource(ownName, new Observer<String>() {
            @Override
            public void onChanged(final String label) {
                maybeGeneratePaymentRequest();
            }
        });
        this.paymentRequest.addSource(amount, new Observer<Coin>() {
            @Override
            public void onChanged(final Coin amount) {
                maybeGeneratePaymentRequest();
            }
        });
        this.paymentRequest.addSource(bluetoothMac, new Observer<String>() {
            @Override
            public void onChanged(final String bluetoothMac) {
                maybeGeneratePaymentRequest();
            }
        });
        this.tdcoinUri.addSource(freshReceiveAddress, new Observer<Address>() {
            @Override
            public void onChanged(final Address receiveAddress) {
                maybeGenerateTdcoinUri();
            }
        });
        this.tdcoinUri.addSource(ownName, new Observer<String>() {
            @Override
            public void onChanged(final String label) {
                maybeGenerateTdcoinUri();
            }
        });
        this.tdcoinUri.addSource(amount, new Observer<Coin>() {
            @Override
            public void onChanged(final Coin amount) {
                maybeGenerateTdcoinUri();
            }
        });
    }

    private void maybeGenerateQrCode() {
        final Address address = freshReceiveAddress.getValue();
        if (address != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    qrCode.postValue(
                            Qr.bitmap(uri(address, amount.getValue(), ownName.getValue(), bluetoothMac.getValue())));
                }
            });
        }
    }

    private void maybeGeneratePaymentRequest() {
        final Address address = freshReceiveAddress.getValue();
        if (address != null) {
            final String bluetoothMac = this.bluetoothMac.getValue();
            final String paymentUrl = bluetoothMac != null ? "bt:" + bluetoothMac : null;
            paymentRequest.setValue(PaymentProtocol.createPaymentRequest(Constants.NETWORK_PARAMETERS,
                    amount.getValue(), address, ownName.getValue(), paymentUrl, null).build().toByteArray());
        }
    }

    private void maybeGenerateTdcoinUri() {
        final Address address = freshReceiveAddress.getValue();
        if (address != null) {
            tdcoinUri.setValue(Uri.parse(uri(address, amount.getValue(), ownName.getValue(), null)));
        }
    }

    private String uri(final Address address, final Coin amount, final String label, final String bluetoothMac) {
        final StringBuilder uri = new StringBuilder(TdcoinURI.convertToTdcoinURI(address, amount, label, null));
        if (bluetoothMac != null) {
            uri.append(amount == null && label == null ? '?' : '&');
            uri.append(Bluetooth.MAC_URI_PARAM).append('=').append(bluetoothMac);
        }
        return uri.toString();
    }

    public static class FreshReceiveAddressLiveData extends AbstractWalletLiveData<Address> {
        private Script.ScriptType outputScriptType = null;

        public FreshReceiveAddressLiveData(final WalletApplication application) {
            super(application);
        }

        public void overrideOutputScriptType(final Script.ScriptType outputScriptType) {
            this.outputScriptType = outputScriptType;
        }

        @Override
        public void setValue(final Address address) {
            super.setValue(address);
        }

        @Override
        protected void onWalletActive(final Wallet wallet) {
            maybeLoad();
        }

        private void maybeLoad() {
            if (getValue() == null) {
                final Wallet wallet = getWallet();
                final Script.ScriptType outputScriptType = this.outputScriptType;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        org.tdcoinj.core.Context.propagate(Constants.CONTEXT);
                        postValue(outputScriptType != null ? wallet.freshReceiveAddress(outputScriptType)
                                : wallet.freshReceiveAddress());
                    }
                });
            }
        }
    }
}
