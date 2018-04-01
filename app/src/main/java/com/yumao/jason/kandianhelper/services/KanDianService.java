package com.yumao.jason.kandianhelper.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;


public class KanDianService extends AccessibilityService {
    public static final String TAG = "KanDianService";

    private static final String KD_MAIN_ACTIVITY = "MainActivity";
    private static final String KD_LOGIN_ACTIVITY = "UserLoginActivity";
    private static final String KD_SETTING_ACTIVITY = "CommonWithTitleFullActivity";
    private static final int KD_WATCHED_TOP_COUNT = 1;
    private static final int KD_LIKED_TOP_COUNT = 1;


    private static final String QQ_PACKAGE_PREFIX = "com.tencent";
    private static final String QQ_LOGIN_ACTIVITY = "LoginActivity";
    private static final String QQ_AUTHORITY_ACTIVITY = "AuthorityActivity";

    private static final String TAB_FIRST_PAGE = "首页";
    private static final String TAB_REFRESH = "刷新";
    private static final String TAB_MY = "我的";
    private static final String QQ_LOGIN_BTN_TEXT = "登 录";
    private static final String QQ_LOGIN_AND_AUTHORITY_BTN_TEXT = "登录";
    private static final String QQ_NUM = "245771473";
    private static final String QQ_PASSWORD = "19921115jiaXIN";

    private AccessibilityNodeInfo rootNodeInfo;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private String mCurrentActivityName = KD_MAIN_ACTIVITY;
    private boolean mHasLiked = false;
    private boolean mHasPostClickRunnable = false;
    private boolean mHasPostSlideRunnable = false;
    private boolean mQQNumDone = false;
    private boolean mQQPasswordDone = false;
    private boolean mHasPerformHome = false;

    private int mWatchedCount = 0;
    private int mLikedCount = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent");
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "onAccessibilityEvent type:TYPE_WINDOW_STATE_CHANGED");
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(TAG, "onAccessibilityEvent type:TYPE_WINDOW_CONTENT_CHANGED");
        }

        setCurrentActivityName(event);

        checkIsQQAuthorityPage(event);
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        try {
            ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
            getPackageManager().getActivityInfo(componentName, 0);
            mCurrentActivityName = componentName.flattenToShortString();
            Log.d(TAG, "mCurrentActivityName:" + mCurrentActivityName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "mCurrentActivityName NameNotFoundException");
            mCurrentActivityName = KD_MAIN_ACTIVITY;
        }
    }


    private void checkIsQQAuthorityPage(final AccessibilityEvent event) {
        if (isInQQAuthorityPage()) {
            if (!mHasPerformHome) {
                mHasPerformHome = true;  // TODO: 18/4/2 在退出登录时将mHasPerformHome置为false
                performGlobalAction(GLOBAL_ACTION_HOME);
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                performGlobalAction(GLOBAL_ACTION_RECENTS);
            } else {
                consumeEvent(event);
            }
        } else {
            consumeEvent(event);
        }
        

    }
    
    private void consumeEvent(AccessibilityEvent event) {
        this.rootNodeInfo = getRootInActiveWindow();
        Log.d(TAG, "consumeEvent() rootNodeInfo:" + rootNodeInfo);
        if (rootNodeInfo == null) {
            return;
        }

        //检查是否看够101个视频并且喜欢够15个视频
        Log.d(TAG, "mWatchedCount:" + mWatchedCount + ", mLikedCount:" + mLikedCount);
        // TODO: 18/4/2 退出登录时将mWatchedCount和mLikedCount置为0
        if (isReadyToChangeAccount()) {
            AccessibilityNodeInfo myNode = getNodeByName(TAB_MY);
            Log.d(TAG, "myNode:" + myNode);
            if (myNode != null) {
                myNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        //看点登录页
        if (isInLoginPage()) {
            printAllChild(rootNodeInfo, 0);
            List<AccessibilityNodeInfo> imageList = findAllTargetWidget(rootNodeInfo, "android.widget.ImageView");
            if (imageList != null && imageList.size() > 0) {
                AccessibilityNodeInfo qqNode = imageList.get(0);
                if (qqNode != null) {
                    qqNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
            return;
        }

        //qq登录页
        if (isInQQLoginPage()) {
            printAllChild(rootNodeInfo, 0);
            List<AccessibilityNodeInfo> editList = findAllTargetWidget(rootNodeInfo, "android.widget.EditText");
            if (editList != null && editList.size() > 0) {
                for (int i = 0; i < editList.size(); i++) {
                    AccessibilityNodeInfo qqLoginNode = editList.get(i);
                    Log.d(TAG, "qq login node text before:" + qqLoginNode.getText());
                    if (i == 0) {
                        if (!mQQNumDone) {
                            Bundle bundle = new Bundle();
                            bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, QQ_NUM);
                            qqLoginNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle);
                            mQQNumDone = true;
                        }
                    } else if (i == 1) {
                        if (!mQQPasswordDone) {
                            Bundle bundle = new Bundle();
                            bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, QQ_PASSWORD);
                            qqLoginNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle);
                            mQQPasswordDone = true;
                        }
                    }
                    Log.d(TAG, "qq login node text after:" + qqLoginNode.getText());
                }
            }

            List<AccessibilityNodeInfo> btnList = findAllTargetWidget(rootNodeInfo, "android.widget.Button");
            if (btnList != null && btnList.size() > 0) {
                for (int i = 0; i < btnList.size(); i++) {
                    AccessibilityNodeInfo loginBtn = btnList.get(i);
                    Log.d(TAG, "loginBtn text:" + loginBtn.getText());
                    if (QQ_LOGIN_BTN_TEXT.equals(loginBtn.getText().toString())) {
                        loginBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
            return;
        }

        //qq登录授权页
        if (isInQQAuthorityPage()) {
            printAllChild(rootNodeInfo, 0);
            List<AccessibilityNodeInfo> btnList = findAllTargetWidget(rootNodeInfo, "android.widget.Button");
            if (btnList != null && btnList.size() > 0) {
                for (int i = 0; i < btnList.size(); i++) {
                    AccessibilityNodeInfo loginBtn = btnList.get(i);
                    Log.d(TAG, "loginBtn text:" + loginBtn.getText());
                    if (QQ_LOGIN_AND_AUTHORITY_BTN_TEXT.equals(loginBtn.getText().toString())) {
                        loginBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
            return;
        }

        //在我的tab下并且需要切换账号
        if (isInMyPage() && isReadyToChangeAccount()) {
            printAllChild(rootNodeInfo, 0);
        }

        //在刷新tab下，即视频播放页
        if (isInVideoPage()) {
            //找喜欢Layout,有两个子View,一个ImageView,一个TextView
            List<AccessibilityNodeInfo> LlList = findAllTargetWidget(rootNodeInfo, "android.widget.LinearLayout");
            AccessibilityNodeInfo likeNode = getTarLl(LlList);
            Log.d(TAG, "likeNode:" + likeNode);
            if (likeNode != null) {
                Log.d(TAG, "mHasLiked:" + mHasLiked);
                if (mHasLiked) {
                    Log.d(TAG, "mHasPostSlideRunnable:" + mHasPostSlideRunnable);
                    if (!mHasPostSlideRunnable) {
                        mHasPostSlideRunnable = true;
                        //已经喜欢过了，3s后滑动页面进入下一个视频
                        Log.d(TAG, "-------------------post slide runnable-----------------");
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                slidePage();
                            }
                        }, 3000);
                    }
                } else {
                    Log.d(TAG, "mHasPostClickRunnable:" + mHasPostClickRunnable);
                    if (!mHasPostClickRunnable) {
                        mHasPostClickRunnable = true;
                        //还没喜欢过，3s后点击喜欢
                        Log.d(TAG, "--------------------post click runnable----------------");
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                clickLikeButton();
                            }
                        }, 3000);
                    }
                }
            } else {
                Log.d(TAG, "没找到喜欢按钮!!!");
                // TODO: 18/3/25 双击手势？
            }
        }
    }

    private boolean isReadyToChangeAccount() {
        return mWatchedCount > KD_WATCHED_TOP_COUNT && mLikedCount > KD_LIKED_TOP_COUNT;
    }

    private boolean isInLoginPage() {
        return mCurrentActivityName.contains(KD_LOGIN_ACTIVITY);
    }

    private boolean isInQQLoginPage() {
        return mCurrentActivityName.contains(QQ_PACKAGE_PREFIX) && mCurrentActivityName.contains(QQ_LOGIN_ACTIVITY);
    }

    private boolean isInQQAuthorityPage() {
        return mCurrentActivityName.contains(QQ_PACKAGE_PREFIX) && mCurrentActivityName.contains(QQ_AUTHORITY_ACTIVITY);
    }

    private boolean isInVideoPage() {
        if (this.rootNodeInfo == null) {
            return false;
        }
        /* 应用首页 */
        if (mCurrentActivityName.contains(KD_MAIN_ACTIVITY)) {
            /* 遍历节点匹配刷新tab" */
            AccessibilityNodeInfo tabRefreshNode = getNodeByName(TAB_REFRESH, TAB_FIRST_PAGE);
            Log.d(TAG, "刷新tab node:" + tabRefreshNode);
            if (tabRefreshNode != null) {
                if (tabRefreshNode.isSelected()) {
                    Log.d(TAG, "当前位置在'刷新tab'或'首页tab'");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInMyPage() {
        if (this.rootNodeInfo == null) {
            return false;
        }
        /* 应用首页 */
        if (mCurrentActivityName.contains(KD_MAIN_ACTIVITY)) {
            /* 遍历节点匹配我的tab" */
            AccessibilityNodeInfo tabMyNode = getNodeByName(TAB_MY);
            Log.d(TAG, "我的tab node:" + tabMyNode);
            if (tabMyNode != null) {
                if (tabMyNode.isSelected()) {
                    Log.d(TAG, "当前位置在'我的tab'");
                    return true;
                }
            }
        }
        return false;
    }

    private void clickLikeButton() {
        List<AccessibilityNodeInfo> LlList = findAllTargetWidget(rootNodeInfo, "android.widget.LinearLayout");
        AccessibilityNodeInfo likeNode = getTarLl(LlList);
        Log.d(TAG, "likeNode:" + likeNode);
        if (likeNode != null) {
            Log.d(TAG, "+++++++++++++++++++click like button++++++++++++");
            likeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mLikedCount++;
        }
        mHasPostClickRunnable = false;
        mHasLiked = true;
    }

    private List<AccessibilityNodeInfo> findAllTargetWidget(AccessibilityNodeInfo node, String className) {
        if (node == null || TextUtils.isEmpty(className)) {
            return null;
        }
        List<AccessibilityNodeInfo> list = new ArrayList<>();

        if (className.equals(node.getClassName())) {
            list.add(node);
        }

        //递归从子View中找
        for (int i = 0; i < node.getChildCount(); i++) {
            List<AccessibilityNodeInfo> listTmp = findAllTargetWidget(node.getChild(i), className);
            if (listTmp != null && listTmp.size() > 0) {
                list.addAll(listTmp);
            }
        }
        return list;
    }

    private void printAllChild(AccessibilityNodeInfo node, int index) {
        if (node == null) {
            return;
        }
        Log.d(TAG, "nodeClass:" + node.getClassName() + ", i:" + index);
        //递归从子View中找
        for (int i = 0; i < node.getChildCount(); i++) {
            printAllChild(node.getChild(i), i);
        }
    }

    private AccessibilityNodeInfo getTarLl(List<AccessibilityNodeInfo> LlList) {
        if (LlList == null || LlList.size() <= 0) {
            return null;
        }
        for (AccessibilityNodeInfo nodeInfo : LlList) {
            if (nodeInfo != null && nodeInfo.getChildCount() == 2) {
                if ("android.widget.FrameLayout".equals(nodeInfo.getChild(0).getClassName())
                        && "android.widget.TextView".equals(nodeInfo.getChild(1).getClassName())) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo getNodeByName(String... texts) {
        AccessibilityNodeInfo tempNode;
        List<AccessibilityNodeInfo> nodes;

        for (String text : texts) {
            if (TextUtils.isEmpty(text)) {
                continue;
            }
            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) {
                for (int i = 0; i < nodes.size(); i++) {
                    tempNode = nodes.get(i);
                    if (tempNode != null) {
                        return tempNode;
                    }
                }
            }
        }
        return null;
    }


    private void slidePage() {
        if (!isInVideoPage()) {
            mHasLiked = false;
            mHasPostSlideRunnable = false;
            Log.d(TAG, "++++++++++++not in video page+++++++++++");
            return;
        }
        Log.d(TAG, "+++++++++slidePage()+++++++++++");
        if (android.os.Build.VERSION.SDK_INT > 23) {

            Path path = new Path();
            path.moveTo(205, 987);
            path.lineTo(255, 211);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 10, 20)).build();
            dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "+++++++++++++++slidePage-onCompleted++++++++++++++");
                    super.onCompleted(gestureDescription);
                    mHasLiked = false;
                    mHasPostSlideRunnable = false;
                    mWatchedCount++;
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "slidePage-onCancelled+++");
                    mHasLiked = false;
                    mHasPostSlideRunnable = false;
                    super.onCancelled(gestureDescription);
                }
            }, null);

        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected");
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

}
