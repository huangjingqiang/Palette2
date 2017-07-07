package cn.han_zi.palette;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.han_zi.palette.bean.SketchData;
import cn.han_zi.palette.bean.StrokeRecord;
import cn.han_zi.palette.view.WhitBoardView;
import pub.devrel.easypermissions.EasyPermissions;

import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static cn.han_zi.palette.bean.StrokeRecord.STROKE_TYPE_ERASER;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, WhitBoardView.OnDrawChangedListener {

    WhitBoardView mSketchView;//画板
    View controlLayout;//控制布局
    ImageView btn_add;//添加画板
    ImageView btn_stroke;//画笔
    ImageView btn_eraser;//橡皮擦
    ImageView btn_undo;//撤销
    ImageView btn_redo;//取消撤销
    ImageView btn_photo;//加载图片
    ImageView btn_background;//背景图片
    ImageView btn_drag;//拖拽
    ImageView btn_save;//保存
    ImageView btn_empty;//清空
    ImageView btn_send;//推送
    ImageView btn_send_space;//推送按钮间隔
    private static final float BTN_ALPHA = 0.4f;

    int strokeMode;//模式
    int strokeType;//模式

    public static int sketchViewHeight;
    public static int sketchViewWidth;
    public static int sketchViewRight;
    public static int sketchViewBottom;
    public static int decorHeight;
    public static int decorWidth;

    int textOffX;
    int textOffY;
    private int size;
    private List<SketchData> sketchDataList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] perms = { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Have permissions, do the thing!
            Toast.makeText(this, "TODO: Location and Contacts things", Toast.LENGTH_LONG).show();
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, "xx",
                    2, perms);
        }

        initView();
        initDrawParams();
        initData();
        strokeType = STROKE_TYPE_DRAW;
        mSketchView.setStrokeType(strokeType);
        mSketchView.setEditMode(WhitBoardView.EDIT_STROKE);
    }

    private void initData() {
        //加载图片
       // mSketchView.addPhotoByPath("");
       // mSketchView.setEditMode(WhitBoardView.EDIT_PHOTO);
        SketchData newSketchData = new SketchData();
        Path strokePath = new Path();
        Paint strokePaint = new Paint();

        int preX=0,preY=0;
        for (int i=0;i<100;i+=10){
            StrokeRecord curStrokeRecord = new StrokeRecord(STROKE_TYPE_DRAW);
            strokePath.quadTo(preX,preY,(i + preX)/2,(i + preY)/2);
            curStrokeRecord.path = strokePath;
            strokePaint.setAntiAlias(true);
            strokePaint.setDither(true);
            strokePaint.setColor(Color.BLACK);
            strokePaint.setStyle(Paint.Style.STROKE);  //描边
            strokePaint.setStrokeJoin(Paint.Join.ROUND);  //圆形画笔
            strokePaint.setStrokeCap(Paint.Cap.ROUND); //圆形画笔
            strokePaint.setStrokeWidth(3);
            curStrokeRecord.paint = new Paint(strokePaint);
            newSketchData.strokeRecordList.add(curStrokeRecord);

            preX = i;
            preY = i+5;
        }
        sketchDataList.add(newSketchData);
        mSketchView.setSketchData(newSketchData);

    }

    private void getSketchSize() {
        ViewTreeObserver vto = mSketchView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (sketchViewHeight == 0 && sketchViewWidth == 0) {
                    int height = mSketchView.getMeasuredHeight();
                    int width = mSketchView.getMeasuredWidth();
                    sketchViewHeight = height;
                    sketchViewWidth = width;
                    sketchViewRight = mSketchView.getRight();
                    sketchViewBottom = mSketchView.getBottom();
                    Log.i("onPreDraw", sketchViewHeight + "  " + sketchViewWidth);
                    decorHeight = getWindow().getDecorView().getMeasuredHeight();
                    decorWidth = getWindow().getDecorView().getMeasuredWidth();
                    Log.i("onPreDraw", "decor height:" + decorHeight + "   width:" + decorHeight);
                    int height3 = controlLayout.getMeasuredHeight();
                    int width3 = controlLayout.getMeasuredWidth();
                    Log.i("onPreDraw", "controlLayout  height:" + height3 + "   width:" + width3);
                }
                return true;
            }
        });
        Log.i("getSketchSize", sketchViewHeight + "  " + sketchViewWidth);
    }

    private void initView() {
        //画板整体布局
        mSketchView = (WhitBoardView)findViewById(R.id.sketch_view);


        controlLayout = findViewById(R.id.controlLayout);

        btn_add = (ImageView) findViewById(R.id.btn_add);
        btn_stroke = (ImageView) findViewById(R.id.btn_stroke);
        btn_eraser = (ImageView) findViewById(R.id.btn_eraser);
        btn_undo = (ImageView) findViewById(R.id.btn_undo);
        btn_redo = (ImageView) findViewById(R.id.btn_redo);
        btn_photo = (ImageView) findViewById(R.id.btn_photo);
        btn_background = (ImageView) findViewById(R.id.btn_background);
        btn_drag = (ImageView) findViewById(R.id.btn_drag);
        btn_save = (ImageView) findViewById(R.id.btn_save);
        btn_empty = (ImageView) findViewById(R.id.btn_empty);
        btn_send = (ImageView) findViewById(R.id.btn_send);
        btn_send_space = (ImageView) findViewById(R.id.btn_send_space);

        /*//设置点击监听
        mSketchView.setOnDrawChangedListener(this);//设置撤销动作监听器
        btn_add.setOnClickListener(this);
        btn_stroke.setOnClickListener(this);
        btn_eraser.setOnClickListener(this);
        btn_undo.setOnClickListener(this);
        btn_redo.setOnClickListener(this);
        btn_empty.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_photo.setOnClickListener(this);
        btn_background.setOnClickListener(this);
        btn_drag.setOnClickListener(this);
        btn_send.setOnClickListener(this);
        mSketchView.setTextWindowCallback(new WhitBoardView.TextWindowCallback() {
            @Override
            public void onText(View anchor, StrokeRecord record) {
                textOffX = record.textOffX;
                textOffY = record.textOffY;
            }
        });*/

        //getSketchSize();
    }

    private void initDrawParams() {
        //默认为画笔模式
        strokeMode = STROKE_TYPE_DRAW;

        //画笔宽度缩放基准参数
        Drawable circleDrawable = getResources().getDrawable(R.drawable.circle);
        assert circleDrawable != null;
        size = circleDrawable.getIntrinsicWidth();
    }

    private void showSketchView(boolean b) {
        mSketchView.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();
        if (id == R.id.btn_add) {
            if (mSketchView.getVisibility() == View.VISIBLE) {
                mSketchView.createCurThumbnailBM();
                showSketchView(false);
            } else {
                showSketchView(true);
            }
        } else if (id == R.id.btn_stroke) {
            if (mSketchView.getEditMode() == WhitBoardView.EDIT_STROKE && mSketchView.getStrokeType() != STROKE_TYPE_ERASER) {
                //showParamsPopupWindow(v, STROKE_TYPE_DRAW);
            } else {
                /*int checkedId = strokeTypeRG.getCheckedRadioButtonId();
                if (checkedId == R.id.stroke_type_rbtn_draw) {
                    strokeType = STROKE_TYPE_DRAW;
                } else if (checkedId == R.id.stroke_type_rbtn_line) {
                    strokeType = STROKE_TYPE_LINE;
                } else if (checkedId == R.id.stroke_type_rbtn_circle) {
                    strokeType = STROKE_TYPE_CIRCLE;
                } else if (checkedId == R.id.stroke_type_rbtn_rectangle) {
                    strokeType = STROKE_TYPE_RECTANGLE;
                } else if (checkedId == R.id.stroke_type_rbtn_text) {
                    strokeType = STROKE_TYPE_TEXT;
                }*/
                strokeType = STROKE_TYPE_DRAW;
                mSketchView.setStrokeType(strokeType);
            }
            mSketchView.setEditMode(WhitBoardView.EDIT_STROKE);
            showBtn(btn_stroke);
        } else if (id == R.id.btn_eraser) {
            if (mSketchView.getEditMode() == WhitBoardView.EDIT_STROKE && mSketchView.getStrokeType() == STROKE_TYPE_ERASER) {
            } else {
                mSketchView.setStrokeType(STROKE_TYPE_ERASER);
            }
            mSketchView.setEditMode(WhitBoardView.EDIT_STROKE);
            showBtn(btn_eraser);
        } else if (id == R.id.btn_undo) {
            mSketchView.undo();
        } else if (id == R.id.btn_redo) {
            mSketchView.redo();
        } else if (id == R.id.btn_empty) {
            //askForErase();
        } else if (id == R.id.btn_save) {
            /*if (mSketchView.getRecordCount() == 0) {
                Toast.makeText(getActivity(), "您还没有绘图", Toast.LENGTH_SHORT).show();
            } else {
                showSaveDialog();
            }*/
        } else if (id == R.id.btn_photo) {
            //startMultiImageSelector(REQUEST_IMAGE);
        } else if (id == R.id.btn_background) {
            // startMultiImageSelector(REQUEST_BACKGROUND);
        } else if (id == R.id.btn_drag) {
            mSketchView.setEditMode(WhitBoardView.EDIT_PHOTO);
            showBtn(btn_drag);
        } else if (id == R.id.btn_send) {
            /*if (sendBtnCallback != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String photoName = TEMP_FILE_NAME + TimeUtils.getNowTimeString();
                        sendBtnCallback.onSendBtnClick(saveInOI(TEMP_FILE_PATH, photoName, 50));
                    }
                }).start();
            }*/
        }
    }

    private void showBtn(ImageView iv) {
        btn_eraser.setAlpha(BTN_ALPHA);
        btn_stroke.setAlpha(BTN_ALPHA);
        btn_drag.setAlpha(BTN_ALPHA);
        iv.setAlpha(1f);
    }

    @Override
    public void onDrawChanged() {
        // 撤销
        if (mSketchView.getStrokeRecordCount() > 0)
            btn_undo.setAlpha(1f);
        else
            btn_undo.setAlpha(0.4f);
        // 重做
        if (mSketchView.getRedoCount() > 0)
            btn_redo.setAlpha(1f);
        else
            btn_redo.setAlpha(0.4f);
    }
}
