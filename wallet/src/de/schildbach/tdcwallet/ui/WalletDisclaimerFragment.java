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

import java.util.Set;

import de.schildbach.tdcwallet.R;
import de.schildbach.tdcwallet.service.BlockchainState;
import de.schildbach.tdcwallet.service.BlockchainState.Impediment;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

/**
 * @author Andreas Schildbach
 */
public final class WalletDisclaimerFragment extends Fragment {
    private TextView messageView;

    private WalletDisclaimerViewModel viewModel;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(WalletDisclaimerViewModel.class);
        viewModel.getBlockchainState().observe(this, new Observer<BlockchainState>() {
            @Override
            public void onChanged(final BlockchainState blockchainState) {
                updateView();
            }
        });
        viewModel.getDisclaimerEnabled().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(final Boolean disclaimerEnabled) {
                updateView();
            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        messageView = (TextView) inflater.inflate(R.layout.wallet_disclaimer_fragment, container);
        messageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final WalletActivityViewModel viewModel = ViewModelProviders.of(getActivity())
                        .get(WalletActivityViewModel.class);
                viewModel.showHelpDialog.setValue(new Event<>(R.string.help_safety));
            }
        });
        return messageView;
    }

    private void updateView() {
        final BlockchainState blockchainState = viewModel.getBlockchainState().getValue();
        final boolean showDisclaimer = viewModel.getDisclaimerEnabled().getValue();

        int progressResId = 0;
        if (blockchainState != null) {
            final Set<Impediment> impediments = blockchainState.impediments;
            if (impediments.contains(Impediment.STORAGE))
                progressResId = R.string.blockchain_state_progress_problem_storage;
            else if (impediments.contains(Impediment.NETWORK))
                progressResId = R.string.blockchain_state_progress_problem_network;
        }

        final SpannableStringBuilder text = new SpannableStringBuilder();
        if (progressResId != 0)
            text.append(Html.fromHtml("<b>" + getString(progressResId) + "</b>"));
        if (progressResId != 0 && showDisclaimer)
            text.append('\n');
        if (showDisclaimer)
            text.append(Html.fromHtml(getString(R.string.wallet_disclaimer_fragment_remind_safety)));
        messageView.setText(text);

        final View view = getView();
        final ViewParent parent = view.getParent();
        final View fragment = parent instanceof FrameLayout ? (FrameLayout) parent : view;
        fragment.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
    }
}
