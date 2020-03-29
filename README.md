# FaceDataManage-人脸数据采集与管理

## 1. 算法说明

* 人脸检测：MTCNN
* 人脸表示：MobileFaceNet-ArcFace
* 模型部署：TfLite

## 2. 使用说明

* 点击+ 添加姓名与对应人脸图片，可从相册选择或拍照，对图片人脸检测。
* 点击☆查看人脸姓名列表，点击可选择删除。

## 3.人脸后端服务器

> FaceBackEnd/

使用Flask + nginx 