package com.tangluo.dlib.natives;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by Tzutalin on 2015/10/20.
 * Modify by tangluo on 2018/10/30
 */

/**
 * A VisionDetRet contains all the information identifying the location and confidence value of the detected object in a bitmap.
 */
public final class VisionDetectRect {
    private String mLabel;
    private String mName;
    private float mConfidence;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private ArrayList<Point> mLandmarkPoints = new ArrayList<>();

    VisionDetectRect() {
    }

    /**
     * @param label      Label name
     * @param name       The name of the recognized result
     * @param confidence A confidence factor between 0 and 1. This indicates how certain what has been found is actually the label.
     * @param l          The X coordinate of the left side of the result
     * @param t          The Y coordinate of the top of the result
     * @param r          The X coordinate of the right side of the result
     * @param b          The Y coordinate of the bottom of the result
     */
    public VisionDetectRect(String label, String name, float confidence, int l, int t, int r, int b) {
        mLabel = label;
        mName = name;
        mLeft = l;
        mTop = t;
        mRight = r;
        mBottom = b;
        mConfidence = confidence;
    }

    /**
     * @return The X coordinate of the left side of the result
     */
    public int getLeft() {
        return mLeft;
    }

    /**
     * @return The Y coordinate of the top of the result
     */
    public int getTop() {
        return mTop;
    }

    /**
     * @return The X coordinate of the right side of the result
     */
    public int getRight() {
        return mRight;
    }

    /**
     * @return The Y coordinate of the bottom of the result
     */
    public int getBottom() {
        return mBottom;
    }

    /**
     * @return A confidence factor between 0 and 1. This indicates how certain what has been found is actually the label.
     */
    public float getConfidence() {
        return mConfidence;
    }

    /**
     * @return The label of the result
     */
    public String getLabel() {
        return mLabel;
    }

    /**
     * @return The name of the recognized result
     */
    public String getName() {
        return mName;
    }

    /**
     * Add landmark to the list. Usually, call by jni
     * @param x Point x
     * @param y Point y
     * @return true if adding landmark successfully
     */
    public boolean addLandmark(int x, int y) {
        return mLandmarkPoints.add(new Point(x, y));
    }

    /**
     * Return the list of landmark points
     * @return ArrayList of android.graphics.Point
     */
    public ArrayList<Point> getFaceLandmarks() {
        return mLandmarkPoints;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Left:");
        sb.append(mLeft);
        sb.append(", Top:");
        sb.append(mTop);
        sb.append(", Right:");
        sb.append(mRight);
        sb.append(", Bottom:");
        sb.append(mBottom);
        sb.append(", Label:");
        sb.append(mLabel);
        sb.append(", Name:");
        sb.append(mName);
        return sb.toString();
    }

}
