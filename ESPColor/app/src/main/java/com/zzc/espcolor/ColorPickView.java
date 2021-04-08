package com.zzc.espcolor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/*
 * @author:zzc
 * time:2021/3/31
 * tools:Android Studio
 * function: // 自定义 ColorPickerView
 */

public class ColorPickView extends View {
    private Context context;
    private int bigCircle;      //外圆半径
    private int rudeRadius;     //可移动小球的半径
    private Bitmap bitmapBack;  //背景图片
    private Paint mPaint;       //背景画笔
    private Paint mCenterPaint; //可移动小球背景
    private Point centerPoint;  //中心位置
    private Point mRockPosition;    //小球当前位置
    private OnColorChangedListener listener;    //小球移动监听
    public String colorStr="";

    public ColorPickView(Context context) {
        super(context);
    }

    public ColorPickView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs);
    }

    public ColorPickView(Context context, AttributeSet attrs){
        super(context,attrs);
        this.context = context;
        init(attrs);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener){
        this.listener = listener;
    }

    /**
     * @describe 初始化操作
     * @param attrs
     */
    private void init(AttributeSet attrs) {
        @SuppressLint("CustomViewStyleable") TypedArray types = context.obtainStyledAttributes(attrs,R.styleable.color_picker);
        //可移动小球的颜色
        int centerColor;
        try {
            //外圆半径
            bigCircle = types.getDimensionPixelOffset(R.styleable.color_picker_circle_radius,320);
            //可移动小球半径
            rudeRadius = types.getDimensionPixelOffset(R.styleable.color_picker_center_radius,20);
            //可移动小球的颜色
            centerColor =types.getColor(R.styleable.color_picker_center_color, Color.WHITE);
        }finally {
            types.recycle();    //TypeArray用完需要recycle
        }

        //中心位置坐标
        centerPoint = new Point(bigCircle,bigCircle);
        mRockPosition = new Point(centerPoint);

        //初始化背景画笔和可移动小球的画笔
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mCenterPaint = new Paint();
        mCenterPaint.setColor(centerColor);

        bitmapBack = createColorBitmap(bigCircle*2,bigCircle*2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画背景图
        canvas.drawBitmap(bitmapBack,0,0,null);
        //画中心小球
        canvas.drawCircle(mRockPosition.x,mRockPosition.y,rudeRadius,mCenterPaint);
    }

    private Bitmap createColorBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);

        int colorCount = 12;
        int colorAngleStep = 360 / 12;
        int[] colors = new int[colorCount + 1];
        float[] hsv = new float[]{0f,1f,1f};
        for(int i=0;i<colors.length;i++){
            hsv[0] = 360 - (i * colorAngleStep) % 360;
            colors[i] = Color.HSVToColor(hsv);
        }
        colors[colorCount] = colors[0];
        SweepGradient sweepGradient = new SweepGradient(width / 2, height / 2, colors, null);
        RadialGradient radialGradient = new RadialGradient(width / 2, height / 2, bigCircle, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        ComposeShader composeShader = new ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER);

        mPaint.setShader(composeShader);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawCircle(width / 2, height / 2, bigCircle, mPaint);
        return bitmap;

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event){
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:       //按下
                //小球到中心位置的距离
                int length = getLength(event.getX(), event.getY(), centerPoint.x, centerPoint.y);
                if(length > bigCircle - rudeRadius){
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:       //移动
                length = getLength(event.getX(),event.getY(),centerPoint.x,centerPoint.y);
                if(length <= bigCircle - rudeRadius){
                    mRockPosition.set((int) event.getX(),(int) event.getY());
                }else{
                    mRockPosition = getBorderPoint(centerPoint,new Point((int) event.getX(),(int) event.getY()),bigCircle - rudeRadius);
                }
                break;

            case MotionEvent.ACTION_UP:         //抬起
                break;

            default:
                break;
        }
        getRGB();
        invalidate();               //更新画布
        return true;
    }

    /*
     *转16进制数
     */
    public static String toBrowserHexValue(int number){
        StringBuilder builder = new StringBuilder(Integer.toHexString(number &0xff));
        while (builder.length() < 2){
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

    /*
     *像素转RGB
     */
    private void getRGB() {
        int pixel = bitmapBack.getPixel((int)mRockPosition.x,(int)mRockPosition.y);
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        int a = Color.alpha(pixel);

        //十六进制的颜色字符串
        colorStr = "#" + toBrowserHexValue(r) + toBrowserHexValue(g) + toBrowserHexValue(b);
        if(listener != null){
            listener.onColorChange(a,r,g,b);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //视图大小设置为直径
        setMeasuredDimension(bigCircle*2,bigCircle*2);
    }

    //计算两点之间的位置
    private static int getLength(float x1, float y1, int x2, int y2) {
        return (int) Math.sqrt(Math.pow(x1 - x2,2) + Math.pow(y1 - y2,2));
    }

    //当触摸点超出圆的范围的时候，设置小球边缘位置
    private static Point getBorderPoint(Point a, Point b, int cutRadius) {
        float radian = getRadian(a,b);
        return new Point(a.x + (int) (cutRadius  * Math.cos(radian)),a.x + (int)(cutRadius * Math.sin(radian)));
    }

    //触摸点与中心点之间直线与水平方向的夹角角度
    public static float getRadian(Point a, Point b){
        float lenA = b.x - a.x;
        float lenB = b.y - a.y;
        float lenC = (float) Math.sqrt(lenA * lenA + lenB * lenB);
        float ang = (float) Math.acos(lenA / lenC);
        ang = ang * (b.y < a.y ? -1 : 1);
        return  ang;
    }

    public String getColorStr(){
        return  colorStr;
    }

    public void setColorStr(String colorStr){
        this.colorStr = colorStr;
    }

    //颜色发生变化的回调接口
    public interface OnColorChangedListener {
        void onColorChange(int a, int r, int g, int b);
    }
}
