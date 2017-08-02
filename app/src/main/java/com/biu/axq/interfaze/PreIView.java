package com.biu.axq.interfaze;

import com.biu.axq.net.AppExp;

/**
 * <p>Author: Huajian Jiang
 * <br>Date: 2017/3/8
 * <br>Email: developer.huajianjiang@gmail.com
 */
public interface PreIView {

    void showPrePrepareUi();

    void showPreSuccessUi();

    void showPreFailureUi(AppExp exp);

    void clearPreUi();
}
