package com.hhl.rebound;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * 弹出菜单
 * Created by HanHailong on 16/2/17.
 */
public class PopMenu {
    private PopMenu(Builder builder) {
        this.activity = builder.activity;
        this.menuItems.clear();
        this.menuItems.addAll(builder.itemList);

        this.columnCount = builder.columnCount;
        this.duration = builder.duration;
        this.tension = builder.tension;
        this.friction = builder.friction;
        this.horizontalPadding = builder.horizontalPadding;
        this.verticalPadding = builder.verticalPadding;
        this.popMenuItemListener = builder.popMenuItemListener;

        screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 显示菜单
     */
    public void show() {
        buildAnimateGridLayout();

        if (animateLayout.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) animateLayout.getParent();
            viewGroup.removeView(animateLayout);
        }

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup contentView = (ViewGroup) decorView.findViewById(android.R.id.content);
        contentView.addView(animateLayout);

        //执行显示动画
        showSubMenus(gridLayout);

        isShowing = true;
    }

    /**
     * 隐藏菜单
     */
    public void hide() {
        //先执行消失的动画
        if (isShowing && gridLayout != null) {
            hideSubMenus(gridLayout, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    ViewGroup contentView = (ViewGroup) decorView.findViewById(android.R.id.content);
                    contentView.removeView(animateLayout);
                }
            });
            isShowing = false;
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    /**
     * 构建动画布局
     */
    private void buildAnimateGridLayout() {
        animateLayout = new FrameLayout(activity);

        gridLayout = new GridLayout(activity);
        gridLayout.setColumnCount(columnCount);
        gridLayout.setBackgroundColor(Color.parseColor("#f0ffffff"));

        int hPadding = dp2px(activity, horizontalPadding);
        int vPadding = dp2px(activity, verticalPadding);
        int itemWidth = (screenWidth - (columnCount + 1) * hPadding) / columnCount;

        int rowCount = menuItems.size() % columnCount == 0 ? menuItems.size() / columnCount :
                menuItems.size() / columnCount + 1;

        int topMargin = (screenHeight - (itemWidth + vPadding) * rowCount + vPadding) / 2;

        for (int i = 0; i < menuItems.size(); i++) {
            final int position = i;
            PopSubView subView = new PopSubView(activity);
            PopMenuItem menuItem = menuItems.get(i);
            subView.setPopMenuItem(menuItem);
            subView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (popMenuItemListener != null) {
                        popMenuItemListener.onItemClick(PopMenu.this, position);
                    }
                    hide();
                }
            });

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = itemWidth;
            lp.leftMargin = hPadding;
            if (i / columnCount == 0) {
                lp.topMargin = topMargin;
            } else {
                lp.topMargin = vPadding;
            }
            gridLayout.addView(subView, lp);
        }

        animateLayout.addView(gridLayout);

        closeIv = new ImageView(activity);
        closeIv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        closeIv.setImageResource(R.drawable.tabbar_compose_background_icon_close);
        closeIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        layoutParams.bottomMargin = dp2px(activity, 25);
        animateLayout.addView(closeIv, layoutParams);
    }

    /**
     * show sub menus with animates
     *
     * @param viewGroup
     */
    private void showSubMenus(ViewGroup viewGroup) {
        if (viewGroup == null) return;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            animateViewDirection(view, screenHeight, 0, tension, friction);
        }
    }

    /**
     * hide sub menus with animates
     *
     * @param viewGroup
     * @param listener
     */
    private void hideSubMenus(ViewGroup viewGroup, final AnimatorListenerAdapter listener) {
        if (viewGroup == null) return;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.animate().translationY(screenHeight).setDuration(duration).setListener(listener).start();
        }
    }

    /**
     * 弹簧动画
     *
     * @param v        动画View
     * @param from
     * @param to
     * @param tension  拉力系数
     * @param friction 摩擦力系数
     */
    private void animateViewDirection(final View v, float from, float to, double tension, double friction) {
        Spring spring = mSpringSystem.createSpring();
        spring.setCurrentValue(from);
        spring.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(tension, friction));
        spring.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                v.setTranslationY((float) spring.getCurrentValue());
            }
        });
        spring.setEndValue(to);
    }

    public static class Builder {

        private Activity activity;
        private int columnCount = DEFAULT_COLUMN_COUNT;
        private List<PopMenuItem> itemList = new ArrayList<>();
        private int duration = DEFAULT_DURATION;
        private double tension = DEFAULT_TENSION;
        private double friction = DEFAULT_FRICTION;
        private int horizontalPadding = DEFAULT_HORIZONTAL_PADDING;
        private int verticalPadding = DEFAULT_VERTICAL_PADDING;
        private PopMenuItemListener popMenuItemListener;

        public Builder attachToActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        public Builder columnCount(int count) {
            this.columnCount = count;
            return this;
        }

        public Builder addMenuItem(PopMenuItem menuItem) {
            this.itemList.add(menuItem);
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder tension(double tension) {
            this.tension = tension;
            return this;
        }

        public Builder friction(double friction) {
            this.friction = friction;
            return this;
        }

        public Builder horizontalPadding(int padding) {
            this.horizontalPadding = padding;
            return this;
        }

        public Builder verticalPadding(int padding) {
            this.verticalPadding = padding;
            return this;
        }

        public Builder setOnItemClickListener(PopMenuItemListener listener) {
            this.popMenuItemListener = listener;
            return this;
        }

        public PopMenu build() {
            final PopMenu popMenu = new PopMenu(this);
            return popMenu;
        }
    }

    private int dp2px(Context context, int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    private static final int DEFAULT_COLUMN_COUNT = 3;// 默认的列数为4个
    private static final int DEFAULT_DURATION = 300; // 动画时间
    private static final int DEFAULT_TENSION = 40; // 拉力系数
    private static final int DEFAULT_FRICTION = 5; // 摩擦力系数
    private static final int DEFAULT_HORIZONTAL_PADDING = 40; // item水平之间的间距
    private static final int DEFAULT_VERTICAL_PADDING = 15; // item竖直之间的间距

    private Activity activity;
    private int columnCount;
    private List<PopMenuItem> menuItems = new ArrayList<>();
    private FrameLayout animateLayout;
    private GridLayout gridLayout;
    private ImageView closeIv;
    private int duration;
    private double tension, friction;
    private int horizontalPadding, verticalPadding;
    private int screenWidth, screenHeight;
    private boolean isShowing = false;
    private PopMenuItemListener popMenuItemListener;
    private SpringSystem mSpringSystem  = SpringSystem.create();
}
