package com.rossia.life.scan.tensor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * @author pd_liu on 2018/1/2.
 *         <p>
 *         覆盖视图
 *         </p>
 *         <p>
 *         1、内部实现保存机制
 *         2、当调用{@link #invalidate()} {@link #postInvalidate()}时，开始执行存储中的任务{@link #mCallbacks}
 *         </p>
 */

public class OverlayView extends View {

    private static final String TAG_LOG = "OverlayView";

    private final List<DrawCallback> mCallbacks = new LinkedList<>();

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public OverlayView(Context context) {
        super(context);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     * <p>
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     * @see #View(Context, AttributeSet, int)
     */
    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute. This constructor of View allows subclasses to use their
     * own base style when they are inflating. For example, a Button class's
     * constructor would call this version of the super class constructor and
     * supply <code>R.attr.buttonStyle</code> for <var>defStyleAttr</var>; this
     * allows the theme's button style to modify all of the base view attributes
     * (in particular its background) as well as the Button class's attributes.
     *
     * @param context      The Context the view is running in, through which it can
     *                     access the current theme, resources, etc.
     * @param attrs        The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *                     reference to a style resource that supplies default values for
     *                     the view. Can be 0 to not look for defaults.
     * @see #View(Context, AttributeSet)
     */
    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*
        循环并执行绘制任务
         */
        for (DrawCallback callback : mCallbacks) {
            callback.callback(canvas);
        }
    }

    /**
     * Interface defining the callback for client classes.
     */
    public interface DrawCallback {
        /**
         * Callback
         *
         * @param canvas 画布
         */
        void callback(Canvas canvas);
    }

    public void addCallback(DrawCallback drawCallback) {
        mCallbacks.add(drawCallback);
    }
}
