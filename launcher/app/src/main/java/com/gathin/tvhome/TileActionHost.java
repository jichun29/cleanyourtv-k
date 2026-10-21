package com.gathin.tvhome;

/**
 * Host interface for tile-action dialog callbacks that must run on the Activity
 * (the adapter only holds a Context). K 定制:菜单键弹窗的「移动位置」需要 Activity 进入重排模式。
 */
interface TileActionHost {
    /** Enter reorder mode with the given tile (component flat key) already picked up. */
    void startReorderCarrying(String key);
}
