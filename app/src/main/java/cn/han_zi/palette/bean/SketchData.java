package cn.han_zi.palette.bean;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

import cn.han_zi.palette.view.WhitBoardView;

/**
 * Create by xjs
 * _______date : 17/7/6
 * _______description:
 */
public class SketchData {
    public List<PhotoRecord> photoRecordList;
    public List<StrokeRecord> strokeRecordList;
    public List<StrokeRecord> strokeRedoList;
    public Bitmap thumbnailBM;//缩略图文件
    public Bitmap backgroundBM;
    public int strokeType;
    public int editMode;

    public SketchData() {
        strokeRecordList = new ArrayList<>();
        photoRecordList = new ArrayList<>();
        strokeRedoList = new ArrayList<>();
        backgroundBM = null;
        thumbnailBM = null;
        strokeType = StrokeRecord.STROKE_TYPE_DRAW;
        editMode = WhitBoardView.EDIT_STROKE;
    }

}
