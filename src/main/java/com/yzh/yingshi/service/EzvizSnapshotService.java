package com.yzh.yingshi.service;

/**
 * 萤石摄像头截图服务
 */
public interface EzvizSnapshotService {

    /**
     * 获取摄像头实时截图URL
     *
     * @param deviceSerial 设备序列号
     * @param channelNo    通道号
     * @return 截图URL, 失败返回null
     */
    String captureSnapshot(String deviceSerial, Integer channelNo);

    /**
     * 获取摄像头实时截图的字节数据
     *
     * @param deviceSerial 设备序列号
     * @param channelNo    通道号
     * @return 截图字节数组, 失败返回null
     */
    byte[] captureSnapshotBytes(String deviceSerial, Integer channelNo);
}
