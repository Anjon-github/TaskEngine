package com.colin.framework.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by xhm on 16-5-14.
 */
public class ToastUtil {

    private static Toast mToast;

    public static void toast(Context con, int strId){
        if(con == null){
            PalLog.d("ToastUtil", "con == null");
        }
        toast(con, con.getString(strId));
    }

    public static void toast(Context con, CharSequence chars){
        if(null != mToast){
            mToast.cancel();
        }
        mToast = Toast.makeText(con.getApplicationContext(), chars, Toast.LENGTH_SHORT);
        mToast.show();
    }

}
