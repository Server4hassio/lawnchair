package com.android.launcher3.folder;

public class ClippedFolderIconLayoutRule {

    // Lawnchair: MIUI-style preview. Up to 9 items are shown in a fixed 3x3 grid,
    // always at the same small size, no matter how many items the folder holds.
    public static final int MAX_NUM_ITEMS_IN_PREVIEW = 9;
    private static final int GRID_SIZE = 3;

    // Space around the grid and between cells, as a fraction of the preview size.
    private static final float OUTER_PADDING = 0.15f;
    private static final float CELL_GAP = 0.05f;

    // The max amount of overlap the preview items can go outside of the background bounds.
    public static final float ICON_OVERLAP_FACTOR = 1.125f;

    public static final int EXIT_INDEX = -2;
    public static final int ENTER_INDEX = -3;

    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mIconSize;
    private boolean mIsRtl;
    private float mCellSize;
    private float mPadding;
    private float mGap;

    public void init(int availableSpace, float intrinsicIconSize, boolean rtl) {
        mAvailableSpace = availableSpace;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mPadding = OUTER_PADDING * availableSpace;
        mGap = CELL_GAP * availableSpace;
        mCellSize = (availableSpace - 2 * mPadding - (GRID_SIZE - 1) * mGap) / GRID_SIZE;
    }

    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        float totalScale = scaleForItem(curNumItems);

        if (index == EXIT_INDEX) {
            // Just right of the first row.
            getGridPosition(0, GRID_SIZE, mTmpPoint);
        } else if (index == ENTER_INDEX) {
            // Just right of the last row.
            getGridPosition(GRID_SIZE - 1, GRID_SIZE, mTmpPoint);
        } else if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            // Items beyond those displayed in the preview are animated to the center
            mTmpPoint[0] = mTmpPoint[1] = mAvailableSpace / 2 - (mIconSize * totalScale) / 2;
        } else {
            getGridPosition(index / GRID_SIZE, index % GRID_SIZE, mTmpPoint);
        }

        if (params == null) {
            params = new PreviewItemDrawingParams(mTmpPoint[0], mTmpPoint[1], totalScale);
        } else {
            params.update(mTmpPoint[0], mTmpPoint[1], totalScale);
        }
        return params;
    }

    /** Top-left corner of the cell at (row, col), mirrored for RTL. */
    private void getGridPosition(int row, int col, float[] result) {
        float step = mCellSize + mGap;
        float x = mPadding + col * step;
        if (mIsRtl) {
            x = mAvailableSpace - x - mCellSize;
        }
        result[0] = x;
        result[1] = mPadding + row * step;
    }

    public float scaleForItem(int numItems) {
        // Same size for every item count.
        return mCellSize / mIconSize;
    }

    public float getIconSize() {
        return mIconSize;
    }
}
