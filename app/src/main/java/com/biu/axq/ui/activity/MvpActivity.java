package com.biu.axq.ui.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.biu.axq.contract.BaseContract;
import com.biu.axq.ui.fragment.MvpFragment;
import com.biu.axq.util.Logger;


/**
 * 为了自动互相绑定 MVP 架构模式中的 View 层 和 Presenter 层
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/6/8
 * <br>Email: developer.huajianjiang@gmail.com
 */
public abstract class MvpActivity<V extends MvpFragment<? super P>, P extends BaseContract.BaseIPresenter<? super V>>
        extends BaseActivity
{
    private static final String TAG = MvpActivity.class.getSimpleName();

    protected V mvpView;

    protected P mvpPresenter;

    /**
     * 创建并返回 MVP 层 中的 View 层
     *
     * @return
     */
    @NonNull
    public abstract V mvpView();

    /**
     * 创建并返回 MVP 层 中的 Presenter 层
     *
     * @return
     */
    @NonNull
    public abstract P mvpPresenter();

    @Override
    protected Fragment onCreateFragment() {
        mvpView = mvpView();
        mvpPresenter = mvpPresenter();
        return mvpView;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mvpView = mvpView();
        mvpPresenter = mvpPresenter();

        mvpView.bindPresenter(mvpPresenter);
        mvpPresenter.bindView(mvpView);
    }

    @Override
    protected void onDestroy() {
        // 自动解绑,避免内存泄漏
        mvpPresenter.unbindView();
        super.onDestroy();
    }

}
