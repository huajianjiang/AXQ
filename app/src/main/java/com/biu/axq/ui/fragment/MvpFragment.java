package com.biu.axq.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.biu.axq.contract.BaseContract;
import com.biu.axq.util.Preconditions;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/5/27
 * <br>Email: developer.huajianjiang@gmail.com
 */
public abstract class MvpFragment<P extends BaseContract.BaseIPresenter> extends BaseFragment
        implements BaseContract.BaseIView<P>
{
    private static final String TAG = MvpFragment.class.getSimpleName();
    protected P presenter;

    @Override
    public void bindPresenter(@NonNull P presenter) {
        Preconditions.isNotNull(presenter, "presenter can not be null");
        this.presenter = presenter;
    }

    protected void start() {
        if (presenter != null) {
            presenter.start();
        }
    }

    protected void stop() {
        if (presenter != null) {
            presenter.stop();
        }
    }
}
