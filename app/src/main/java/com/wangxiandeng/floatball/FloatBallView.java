package com.wangxiandeng.floatball;


import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

/**
 * Created by wangxiandeng on 2016/11/25.
 */

public class FloatBallView extends LinearLayout {
    private ImageView mImgBall;
    private ImageView mImgBigBall;
    private ImageView mImgBg;

    private WindowManager mWindowManager;

    private WindowManager.LayoutParams mLayoutParams;

    private long mLastDownTime;
    private float mLastDownX;
    private float mLastDownY;

    private boolean mIsLongTouch;

    private boolean mIsTouching;

    private float mTouchSlop;
    private final static long LONG_CLICK_LIMIT = 300;
    private final static long TO_APP_INDEX_LIMIT = 1500;
    private final static long CLICK_LIMIT = 200;

    private int mStatusBarHeight;

    private AccessibilityService mService;

    private int mCurrentMode;

    private final static int MODE_NONE = 0x000;
    private final static int MODE_DOWN = 0x001;
    private final static int MODE_UP = 0x002;
    private final static int MODE_LEFT = 0x003;
    private final static int MODE_RIGHT = 0x004;
    private final static int MODE_MOVE = 0x005;
    private final static int MODE_TOAPP = 0x006;

    private final static int OFFSET = 30;

    private float mBigBallX;
    private float mBigBallY;

    private int mOffsetToParent;
    private int mOffsetToParentY;
    private Vibrator mVibrator;
    private long[] mPattern = {0, 100};


    public FloatBallView(Context context) {
        super(context);
        mService = (AccessibilityService) context;
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.layout_ball, this);
        mImgBall = (ImageView) findViewById(R.id.img_ball);
        mImgBigBall = (ImageView) findViewById(R.id.img_big_ball);
        mImgBg = (ImageView) findViewById(R.id.img_bg);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mCurrentMode = MODE_NONE;

        mStatusBarHeight = getStatusBarHeight();
        mOffsetToParent = dip2px(25);
        mOffsetToParentY = mStatusBarHeight + mOffsetToParent;

        mImgBigBall.post(new Runnable() {
            @Override
            public void run() {
                mBigBallX = mImgBigBall.getX();
                mBigBallY = mImgBigBall.getY();
            }
        });

        mImgBg.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mIsTouching = true;
                        mImgBall.setVisibility(INVISIBLE);
                        mImgBigBall.setVisibility(VISIBLE);
                        mLastDownTime = System.currentTimeMillis();
                        mLastDownX = event.getX();
                        mLastDownY = event.getY();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!mIsLongTouch && mIsTouching && mCurrentMode == MODE_NONE) {
                                    mIsLongTouch = isLongClick(event);
                                }
                            }
                        }, LONG_CLICK_LIMIT);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!mIsLongTouch && isTouchSlop(event)) {
                            return true;
                        }
                        if (mIsLongTouch && (mCurrentMode == MODE_NONE || mCurrentMode == MODE_MOVE)) {
                            mLayoutParams.x = (int) (event.getRawX() - mOffsetToParent);
                            mLayoutParams.y = (int) (event.getRawY() - mOffsetToParentY);
                            mWindowManager.updateViewLayout(FloatBallView.this, mLayoutParams);
                            mBigBallX = mImgBigBall.getX();
                            mBigBallY = mImgBigBall.getY();
                            mCurrentMode = MODE_MOVE;
                        } else {
                            doGesture(event);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        mIsTouching = false;
                        if (mIsLongTouch) {
                            mIsLongTouch = false;
                        } else if (isClick(event)) {
                            AccessibilityUtil.doBack(mService);
                        } else {
                            doUp();
                        }
                        mImgBall.setVisibility(VISIBLE);
                        mImgBigBall.setVisibility(INVISIBLE);
                        mCurrentMode = MODE_NONE;
                        break;
                }
                return true;
            }
        });
    }

    private void toAppIndex() {
        mVibrator.vibrate(mPattern, -1);
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    private boolean isTouchSlop(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (Math.abs(x - mLastDownX) < mTouchSlop && Math.abs(y - mLastDownY) < mTouchSlop) {
            return true;
        }
        return false;
    }

    private void doGesture(MotionEvent event) {
        float offsetX = event.getX() - mLastDownX;
        float offsetY = event.getY() - mLastDownY;

        if (Math.abs(offsetX) < mTouchSlop && Math.abs(offsetY) < mTouchSlop) {
            return;
        }
        if (Math.abs(offsetX) > Math.abs(offsetY)) {
            if (offsetX > 0) {
                if (mCurrentMode == MODE_RIGHT) {
                    return;
                }
                mCurrentMode = MODE_RIGHT;
                mImgBigBall.setX(mBigBallX + OFFSET);
                mImgBigBall.setY(mBigBallY);
            } else {
                if (mCurrentMode == MODE_LEFT) {
                    return;
                }
                mCurrentMode = MODE_LEFT;
                mImgBigBall.setX(mBigBallX - OFFSET);
                mImgBigBall.setY(mBigBallY);
            }
        } else {
            if (offsetY > 0) {
                if (mCurrentMode == MODE_DOWN || mCurrentMode == MODE_TOAPP) {
                    return;
                }
                mCurrentMode = MODE_DOWN;
                mImgBigBall.setX(mBigBallX);
                mImgBigBall.setY(mBigBallY + OFFSET);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentMode == MODE_DOWN && mIsTouching) {
                            if (MainActivity.mIsShowing) {
                                return;
                            }
                            toAppIndex();
                            mCurrentMode = MODE_TOAPP;
                        }
                    }
                }, TO_APP_INDEX_LIMIT);
            } else {
                if (mCurrentMode == MODE_UP) {
                    return;
                }
                mCurrentMode = MODE_UP;
                mImgBigBall.setX(mBigBallX);
                mImgBigBall.setY(mBigBallY - OFFSET);
            }
        }
    }

    private void doUp() {
        switch (mCurrentMode) {
            case MODE_LEFT:
            case MODE_RIGHT:
                AccessibilityUtil.doLeftOrRight(mService);
                break;
            case MODE_DOWN:
                AccessibilityUtil.doPullDown(mService);
                break;
            case MODE_UP:
                AccessibilityUtil.doPullUp(mService);
                break;

        }
        mImgBigBall.setX(mBigBallX);
        mImgBigBall.setY(mBigBallY);
    }

    public void setLayoutParams(WindowManager.LayoutParams params) {
        mLayoutParams = params;
    }

    private boolean isLongClick(MotionEvent event) {
        float offsetX = Math.abs(event.getX() - mLastDownX);
        float offsetY = Math.abs(event.getY() - mLastDownY);
        long time = System.currentTimeMillis() - mLastDownTime;

        if (offsetX < mTouchSlop && offsetY < mTouchSlop && time >= LONG_CLICK_LIMIT) {
            mVibrator.vibrate(mPattern, -1);
            return true;
        } else {
            return false;
        }
    }

    private boolean isClick(MotionEvent event) {
        float offsetX = Math.abs(event.getX() - mLastDownX);
        float offsetY = Math.abs(event.getY() - mLastDownY);
        long time = System.currentTimeMillis() - mLastDownTime;

        if (offsetX < mTouchSlop * 2 && offsetY < mTouchSlop * 2 && time < CLICK_LIMIT) {
            return true;
        } else {
            return false;
        }
    }

    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            statusBarHeight = getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    public int dip2px(float dip) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dip, getContext().getResources().getDisplayMetrics()
        );
    }

}
