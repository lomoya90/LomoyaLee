package com.duapps.rec.notification;

import android.app.Notification;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v7.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.duapps.rec.R;
import com.duapps.rec.config.DuRecorderConfig;
import com.duapps.rec.config.FeatureConfig;
import com.duapps.rec.utils.LogHelper;

/**
 * Created by liyan36 on 2016-05-26.
 */
public class NotificationTextColorUtils {

    private static final String TAG = "NotificationTextColorUtils";

    /**
     * 遍历查找对应title，text2的TextView，并获取defaultColor进行保存
     *
     * @param context
     * @param contentView
     * @param resEntryName
     */
    private static void saveSystemNTDefaultColor(Context context,
                                                 ViewGroup contentView, String resEntryName) {
        int defaultColor = -1;
        int count = contentView.getChildCount();
        for (int i = 0; i < count; i++) {
            if (contentView.getChildAt(i) instanceof ViewGroup) {
                saveSystemNTDefaultColor(context, (ViewGroup) contentView.getChildAt(i), resEntryName);
            } else if (contentView.getChildAt(i) instanceof TextView) {
                View view = contentView.getChildAt(i);
                try {
                    String idName = context.getResources().getResourceEntryName(view.getId());
                    if (resEntryName.equals(idName)) {
                        defaultColor = ((TextView) view).getTextColors().getDefaultColor();
                        DuRecorderConfig.saveNTTextViewDefaultColor(resEntryName,
                                "#" + Integer.toHexString(defaultColor));
                        break;
                    }
                } catch (Resources.NotFoundException e) {
                    if (FeatureConfig.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 获取系统通知的文字颜色
     *
     * @param context
     * @param resId          为android.R.id.text2时，获取通知内容文字颜色；
     *                       为android.R.id.title时，获取通知标题文字颜色；
     * @param defaultStyleId 默认的系统通知文字颜色style属性
     * @return "#000000"颜色值
     */
    private static String getNotificationFontColor(Context context, int resId, int defaultStyleId) {
        // 设置自定义通知的标题和内容颜色
        Resources res = context.getResources();
        try {
            String textEntryName = res.getResourceEntryName(resId);
            String textColor = DuRecorderConfig.getNTTextViewDefaultColor(textEntryName);
            if (textColor == null) { // 颜色值为空时，取系统的通知内容颜色
                initNotificationTextDefaultColor(context, resId);
                textColor = DuRecorderConfig.getNTTextViewDefaultColor(textEntryName);

                if (textColor == null) { // 获取颜色值失败
                    int[] attrs = new int[]{android.R.attr.textColor};
                    final TypedArray textTypedArray = context.obtainStyledAttributes(
                            defaultStyleId, attrs);
                    ColorStateList textColStateList = textTypedArray.getColorStateList(0);
                    if (textColStateList != null) {
                        textColor = "#" + Integer.toHexString(textColStateList.getDefaultColor());
                    } else { // 不可能存在
                        textColor = "#";
                    }
                    LogHelper.d(TAG, "text color:" + textColor);
                    textTypedArray.recycle();
                    DuRecorderConfig.saveNTTextViewDefaultColor(textEntryName, textColor);
                }
            }
            return textColor;
        } catch (Resources.NotFoundException e) {
            if (FeatureConfig.DEBUG) {
                e.printStackTrace();
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (FeatureConfig.DEBUG) {
                e.printStackTrace();
            }
            return null;
        }

    }

    /**
     * 初始化当前手机通知栏默认颜色
     */
    private static void initNotificationTextDefaultColor(Context context, int resId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Notification n = builder.build();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            ViewGroup contentView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
            // 系统通知内容TextView的id
            String textResEntryName = context.getResources().getResourceEntryName(resId);
            saveSystemNTDefaultColor(context, contentView, textResEntryName);
        } catch (Resources.NotFoundException e) {
            if (FeatureConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取通知内容文字颜色
     *
     * @param context
     * @return
     */
    public static String getNotificationTextColor(Context context) {
        return getNotificationFontColor(context, android.R.id.text2,
                R.style.DurecCustomNotificationContentTextAppearance);
    }

    /**
     * 获取通知内容标题颜色
     *
     * @param context
     * @return
     */
    public static String getNotificationTitleColor(Context context) {
        return getNotificationFontColor(context, android.R.id.title,
                R.style.DurecCustomNotificationTitleTextAppearance);
    }
}
