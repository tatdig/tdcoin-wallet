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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schildbach.tdcwallet.R;
import de.schildbach.tdcwallet.WalletApplication;

import android.app.ActivityManager.TaskDescription;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractWalletActivity extends FragmentActivity {
    private WalletApplication application;

    protected static final Logger log = LoggerFactory.getLogger(AbstractWalletActivity.class);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        application = (WalletApplication) getApplication();
        setTaskDescription(new TaskDescription(null, null, ContextCompat.getColor(this, R.color.bg_action_bar)));
        super.onCreate(savedInstanceState);
    }

    public WalletApplication getWalletApplication() {
        return application;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setShowWhenLocked(final boolean showWhenLocked) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            super.setShowWhenLocked(showWhenLocked);
        else if (showWhenLocked)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }
}
