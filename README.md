# Sensor-Measurement-System

整个项目的软件开发部分主要分为两大部分：一是单片机数据采集以及蓝牙传输部分，而是Andriod APP读取蓝牙数据并动态显示

一：数据采集与传输部分
    1.选用STM32F103C8T6芯片，使用ADC对电阻式传感器信号进行连续采集。
    2.选用HC05作为蓝牙传输模块，采用AT指令对该模块进行设置与控制。
    3.在设计过程中涉及到的硬件驱动部分主要参考正点原子代码。
    
二：Android APP部分
    1.APP工程使用Eclipse开发，包含两个主要的activity：主界面Activity为蓝牙
      https://github.com/blacksmith0925/images_added/blob/master/screenshots/_20190728140437.jpg
      
      之后跳转到数据动态显示界面
      https://github.com/blacksmith0925/images_added/blob/master/screenshots/14c8b88da7c528b73c7f4df34b4fc52.jpg
      
    2.适用于Android 5.0(API 22)及以上版本。 
