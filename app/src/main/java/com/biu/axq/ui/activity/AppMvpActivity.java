package com.biu.axq.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.biu.axq.R;
import com.biu.axq.contract.BaseContract;
import com.biu.axq.ui.fragment.MvpFragment;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/5/15
 * <br>Email: developer.huajianjiang@gmail.com
 */
public abstract class AppMvpActivity<V extends MvpFragment<? super P>, P extends BaseContract.BaseIPresenter<? super V>>
        extends MvpActivity<V, P>
{
    /**
     * 居中显示在 Toolbar 中的标题文本控件
     */
    private TextView mTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSysActionbarTitle();
        mTitle = setCustomViewForToolbar(R.layout.toolbar_title);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getString(titleId));
    }

}
