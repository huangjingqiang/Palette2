package cn.han_zi.palette.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import cn.han_zi.palette.bean.PhotoRecord;
import cn.han_zi.palette.bean.SketchData;
import cn.han_zi.palette.bean.StrokeRecord;
import cn.han_zi.palette.utils.BitmapUtils;
import cn.han_zi.palette.utils.ScreenUtils;

import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_LINE;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_TEXT;
import static cn.han_zi.palette.utils.BitmapUtils.createBitmapThumbnail;

/**
 * Create by xjs
 * _______date : 17/7/5
 * _______description:白板自定义view
 */
public class WhitBoardView extends View implements View.OnTouchListener {
    public static final int EDIT_STROKE = 1;
    public static final int EDIT_PHOTO = 2;
    public static final int DEFAULT_STROKE_SIZE = 3;
    public static final int DEFAULT_ERASER_SIZE = 50;

    private Context mContext;
    public int actionMode;
    public SketchData curSketchData;
    public StrokeRecord curStrokeRecord;
    public PhotoRecord curPhotoRecord;
    public Path strokePath;
    public TextWindowCallback textWindowCallback;
    public static final int ACTION_DRAG = 1; //平移
    public static final int ACTION_SCALE = 2; //缩放
    private Paint strokePaint;     //画笔
    public int strokeRealColor = Color.BLACK;//画笔颜色
    public float strokeSize = DEFAULT_STROKE_SIZE; //画笔宽度
    public float eraserSize = DEFAULT_ERASER_SIZE; //橡皮擦
    int[] location = new int[2];  //当前窗口坐标
    public float downX; //手指按下x坐标
    public float downY; //手指按下y坐标
    public float preX;  //临时x坐标
    public float preY;   //临时y坐标 （判断手指是否移动）
    public float curX;  //当前X坐标
    public float curY;  //当前Y坐标
    public int mWidth, mHeight;
    public int drawDensity = 2;//绘制密度,数值越高图像质量越低、性能越好
    public Bitmap tempBitmap;//临时绘制的bitmap
    public Canvas tempCanvas;
    public Bitmap tempHoldBitmap;//保存已固化的笔画bitmap
    public Canvas tempHoldCanvas;
    public OnDrawChangedListener onDrawChangedListener;

    public void setTextWindowCallback(TextWindowCallback textWindowCallback){
        this.textWindowCallback = textWindowCallback;
    }

    public void setOnDrawChangedListener(OnDrawChangedListener listener){
        this.onDrawChangedListener = listener;
    }

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
        curPhotoRecord = null;
        invalidate();
    }

    public void setStrokeType(int strokeType) {
        this.curSketchData.strokeType = strokeType;
    }

    public void setEditMode(int editMode) {
        this.curSketchData.editMode = editMode;
        invalidate();
    }

    public int getEditMode() {
        return curSketchData.editMode;
    }

    public int getStrokeType() {
        return curSketchData.strokeType;
    }

    public void addPhotoByPath(String path) {
        Glide.with(mContext)
                .load("http://www.fotor.com/images2/features/photo_effects/e_bw.jpg")
                .asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                addPhotoByBitmap(resource);
            }
        });
    }

    @NonNull
    public Bitmap getResultBitmap() {
        return getResultBitmap(null);
    }

    @NonNull
    public Bitmap getResultBitmap(Bitmap addBitmap) {
        Bitmap newBM = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBM);
        //绘制背景
        drawBackground(canvas);
        drawRecord(canvas, false);

        if (addBitmap != null) {
            canvas.drawBitmap(addBitmap, 0, 0, null);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        Bitmap bitmap = BitmapUtils.createBitmapThumbnail(newBM, true, 800, 1280);
        return bitmap;
    }

    @NonNull
    public void createCurThumbnailBM() {
        curSketchData.thumbnailBM = getThumbnailResultBitmap();
    }

    @NonNull
    public Bitmap getThumbnailResultBitmap() {
        return createBitmapThumbnail(getResultBitmap(), true, ScreenUtils.dip2px(mContext, 200), ScreenUtils.dip2px(mContext, 200));
    }

    public void addPhotoByBitmap(Bitmap sampleBM) {
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void setCurPhotoRecord(PhotoRecord record) {
        curSketchData.photoRecordList.remove(record);
        curSketchData.photoRecordList.add(record);
        curPhotoRecord = record;
        invalidate();
    }

    @NonNull
    public PhotoRecord initPhotoRecord(Bitmap bitmap) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(getWidth() / 2 - bitmap.getWidth() / 2, getHeight() / 2 - bitmap.getHeight() / 2);
        return newRecord;
    }

    public float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
    }

    /*
     * 删除一笔
     */
    public void undo() {
        if (curSketchData.strokeRecordList.size() > 0) {
            curSketchData.strokeRedoList.add(curSketchData.strokeRecordList.get(curSketchData.strokeRecordList.size() - 1));
            curSketchData.strokeRecordList.remove(curSketchData.strokeRecordList.size() - 1);
            invalidate();
        }
    }

    /*
     * 撤销
     */
    public void redo() {
        if (curSketchData.strokeRedoList.size() > 0) {
            curSketchData.strokeRecordList.add(curSketchData.strokeRedoList.get(curSketchData.strokeRedoList.size() - 1));
            curSketchData.strokeRedoList.remove(curSketchData.strokeRedoList.size() - 1);
        }
        invalidate();
    }

    public int getRedoCount() {
        return curSketchData.strokeRedoList != null ? curSketchData.strokeRedoList.size() : 0;
    }

    public int getRecordCount() {
        return (curSketchData.strokeRecordList != null && curSketchData.photoRecordList != null) ? curSketchData.strokeRecordList.size() + curSketchData.photoRecordList.size() : 0;
    }

    public int getStrokeRecordCount() {
        return curSketchData.strokeRecordList != null ? curSketchData.strokeRecordList.size() : 0;
    }

    public WhitBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initParams(context);
        if (isFocusable()){
            this.setOnTouchListener(this);
        }
        invalidate();
    }

    private void initParams(Context context) {
        setBackgroundColor(Color.WHITE);

        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);  //抗锯齿
        strokePaint.setDither(true);    //防抖动
        strokePaint.setColor(strokeRealColor);
        strokePaint.setStyle(Paint.Style.STROKE);  //描边
        strokePaint.setStrokeJoin(Paint.Join.ROUND);  //圆形画笔
        strokePaint.setStrokeCap(Paint.Cap.ROUND); //圆形画笔
        strokePaint.setStrokeWidth(strokeSize);
    }

    @Override
    protected void onMeasure(int widthMeasurepec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasurepec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth,mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawRecord(canvas);
        if (onDrawChangedListener != null)
            onDrawChangedListener.onDrawChanged();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        getLocationInWindow(location);      //获取当前窗口绝对坐标
        curX = (motionEvent.getRawX() - location[0]) / drawDensity;
        curY = (motionEvent.getRawY() - location[1]) / drawDensity;
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                float downDistance = spacing(motionEvent); //绝对距离
                if (actionMode == ACTION_DRAG && downDistance > 10)   //防止误触
                    actionMode = ACTION_SCALE;
                break;
            case MotionEvent.ACTION_DOWN:
                touch_down();
                invalidate(); //重绘
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;

        }
        preX = curX;
        preY = curY;
        return true;
    }

    /**
     * 绘制背景
     * @param canvas
     */
    public void drawBackground(Canvas canvas){
        if (curSketchData.backgroundBM != null){
            Matrix matrix = new Matrix();
            float wScale = canvas.getWidth() / curSketchData.backgroundBM.getWidth();
            float hScale = canvas.getHeight() / curSketchData.backgroundBM.getHeight();
            matrix.postScale(wScale,hScale);
            canvas.drawBitmap(curSketchData.backgroundBM,matrix,null);
        }else {
            canvas.drawColor(Color.rgb(239,234,224));
        }
    }

    /**
     * 绘制记录数据
     */
    public void drawRecord(Canvas canvas){
        drawRecord(canvas, true);
    }

    public void drawRecord(Canvas canvas,boolean isDrawBoard){
        if (curSketchData != null){
            for (PhotoRecord record : curSketchData.photoRecordList){
                if (record != null){
                    canvas.drawBitmap(record.bitmap,record.matrix,null);
                }
            }
            if (isDrawBoard && curSketchData.editMode == EDIT_PHOTO && curPhotoRecord != null){
                //绘制图形边角线
            }
            /**
             * 新建临时画布，使用橡皮擦
             */
            if (tempBitmap == null){
                tempBitmap = Bitmap.createBitmap(getWidth() / drawDensity,getHeight() / drawDensity,Bitmap.Config.ARGB_4444);
                tempCanvas = new Canvas(tempBitmap);
            }
            /**
             * 新建临时画布，保存过多画笔
             */
           if (tempHoldBitmap == null){
               tempHoldBitmap = Bitmap.createBitmap(getWidth() / drawDensity,getHeight() / drawDensity,Bitmap.Config.ARGB_4444);
               tempHoldCanvas = new Canvas(tempHoldBitmap);
           }
            //把十个操作以前的笔画全都画进固化层
            while (curSketchData.strokeRecordList.size() > 10) {
                StrokeRecord record = curSketchData.strokeRecordList.get(0);
                int type = record.type;
                if (type == StrokeRecord.STROKE_TYPE_ERASER) {//橡皮擦需要在固化层也绘制
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == STROKE_TYPE_CIRCLE) {
                    tempHoldCanvas.drawOval(record.rect, record.paint);
                } else if (type == STROKE_TYPE_RECTANGLE) {
                    tempHoldCanvas.drawRect(record.rect, record.paint);
                } else if (type == STROKE_TYPE_TEXT) {
                    if (record.text != null) {
                        StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                        tempHoldCanvas.translate(record.textOffX, record.textOffY);
                        layout.draw(tempHoldCanvas);
                        tempHoldCanvas.translate(-record.textOffX, -record.textOffY);
                    }
                }
                curSketchData.strokeRecordList.remove(0);
            }
            clearCanvas(tempCanvas);//清空画布
            tempCanvas.drawColor(Color.TRANSPARENT);
            tempCanvas.drawBitmap(tempHoldBitmap, new Rect(0, 0, tempHoldBitmap.getWidth(), tempHoldBitmap.getHeight()), new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), null);
            for (StrokeRecord record : curSketchData.strokeRecordList) {
                int type = record.type;
                if (type == StrokeRecord.STROKE_TYPE_ERASER) {//橡皮擦需要在固化层也绘制
                    tempCanvas.drawPath(record.path, record.paint);
                    tempHoldCanvas.drawPath(record.path, record.paint);
                } else if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
                    tempCanvas.drawPath(record.path, record.paint);
                } else if (type == STROKE_TYPE_CIRCLE) {
                    tempCanvas.drawOval(record.rect, record.paint);
                } else if (type == STROKE_TYPE_RECTANGLE) {
                    tempCanvas.drawRect(record.rect, record.paint);
                } else if (type == STROKE_TYPE_TEXT) {
                    if (record.text != null) {
                        StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                        tempCanvas.translate(record.textOffX, record.textOffY);
                        layout.draw(tempCanvas);
                        tempCanvas.translate(-record.textOffX, -record.textOffY);
                    }
                }
            }
            canvas.drawBitmap(tempBitmap, new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
        }
    }

    /**
     * 清理画布canvas
     * @param temptCanvas
     */
    public void clearCanvas(Canvas temptCanvas) {
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        temptCanvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    /**
     * 计算绝对距离
     *
     * @param event
     * @return
     */
    public float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 手指按下事件
     */
    public void touch_down() {
        downX = curX;
        downY = curY;
        if (curSketchData.editMode == EDIT_STROKE) {
            curSketchData.strokeRedoList.clear();
            curStrokeRecord = new StrokeRecord(curSketchData.strokeType);
            strokePaint.setAntiAlias(true);
            if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));   //设置图像混合模式，CLEAR不绘制到画布上
            } else {
                strokePaint.setXfermode(null);
            }
            if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                strokePaint.setColor(Color.WHITE);
                strokePaint.setStrokeWidth(eraserSize);
                curStrokeRecord.paint = new Paint(strokePaint);
                curStrokeRecord.path = strokePath;
            } else if (curSketchData.strokeType == STROKE_TYPE_DRAW || curSketchData.strokeType == STROKE_TYPE_LINE) {
                strokePath = new Path();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.path = strokePath;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint);
            } else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(downX, downY, downX, downY);
                curStrokeRecord.rect = rect;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint);
            } else if (curSketchData.strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) downX;
                curStrokeRecord.textOffY = (int) downY;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp;
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            curSketchData.strokeRecordList.add(curStrokeRecord);
        } else if (curSketchData.editMode == EDIT_PHOTO) {
            //操作背景图
        }

    }

    /**
     * 手指移动事件
     */
    public void touch_move(){
        if (curSketchData.editMode == EDIT_STROKE){
            if (curSketchData.strokeType == STROKE_TYPE_ERASER){
                strokePath.quadTo(preX,preY,(curX + preX)/2,(curY + preY)/2);
            }else if (curSketchData.strokeType == STROKE_TYPE_DRAW){
                strokePath.quadTo(preX,preY,(curX + preX)/2,(curY + preY)/2);
            }else if (curSketchData.strokeType == STROKE_TYPE_LINE){
                strokePath.reset();
                strokePath.moveTo(downX,downY);
                strokePath.lineTo(curX,curY);
            }else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE){
                curStrokeRecord.rect.set(downX < curX ? downX : curX, downY < curY ? downY : curY, downX > curX ? downX : curX, downY > curY ? downY : curY);
            }else if (curSketchData.strokeType == STROKE_TYPE_TEXT){

            }
        }else if (curSketchData.editMode == EDIT_PHOTO && curStrokeRecord != null){

        }
        preX = curX;
        preY = curY;
    }

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }
    public interface OnDrawChangedListener {
         void onDrawChanged();
    }
}
