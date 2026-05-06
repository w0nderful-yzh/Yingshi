package com.yzh.yingshi.service;

import java.util.List;

/**
 * 宠物AI检测接口 (可扩展)
 * 当前提供Mock实现, 后续可接入YOLOv8等真实AI模型
 */
public interface PetAiDetector {

    /**
     * 检测图片中的宠物
     *
     * @param imageUrl 图片URL
     * @return 检测到的宠物列表
     */
    List<PetDetection> detect(String imageUrl);

    /**
     * 宠物检测结果
     */
    class PetDetection {
        /** 宠物类型 (dog/cat/pet) */
        private String type;
        /** 置信度 0-1 */
        private double confidence;
        /** 边界框左上角X (百分比 0-100) */
        private double x;
        /** 边界框左上角Y (百分比 0-100) */
        private double y;
        /** 边界框宽度 (百分比 0-100) */
        private double width;
        /** 边界框高度 (百分比 0-100) */
        private double height;

        public PetDetection() {}

        public PetDetection(String type, double confidence, double x, double y, double width, double height) {
            this.type = type;
            this.confidence = confidence;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
    }
}
