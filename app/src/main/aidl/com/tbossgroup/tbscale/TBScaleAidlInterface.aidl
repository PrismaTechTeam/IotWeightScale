package com.tbossgroup.tbscale;

import com.tbossgroup.tbscale.EventBatteryPower;

interface TBScaleAidlInterface {

    //true:重量稳定
    //false:重量不稳定
    boolean isWeightStable();

    //true:净重
    //false:毛重
    boolean setNetGrossShowState();

    //true:净重
    //false:毛重
    boolean isNetWeightShowState();

    //获取净重
    float getNetWeight();
    String getNetWeightString();

    //获取毛重值
    float getGrossWeight();
    String getGrossWeightString();

    //获取扣重
    float getTareWeight();
    String getTareWeightString();

    //归零
    //ture:成功
    //false:失败
    boolean setZero();

    //扣重
    //ture:成功
    //false:失败
    boolean setTare();

    //获得使用单位
    String getUnitString();

    //无单位     NO_UNIT = 0;
    //千克       KG = 2;
    //克         G = 3;
    //磅         LB = 4;
    //市斤       C_JIN = 8;
    //台斤       T_JIN = 13;
    //牛顿       NEWTON = 23;
    int getUnit();

    //获取设备ID
    String getScaleID();

    // 当前单位小数点长度
    int getCurUnitFraclen();

    // 关机
    void shutdown();

    // 电池电量
    EventBatteryPower getBatteryPower();

}